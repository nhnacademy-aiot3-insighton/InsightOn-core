package com.insighton.core.mqtt.connection;

import com.insighton.core.gateway.MqttGatewayConnectionInfo;
import com.insighton.core.gateway.entity.Gateway;
import com.insighton.core.gateway.entity.ProtocolType;
import com.insighton.core.gateway.repository.GatewayRepository;
import com.insighton.core.mqtt.cache.GatewayConnectionInfoCache;
import com.insighton.core.mqtt.cache.GatewayGroupMappingCache;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 다중 인스턴스 환경에서 게이트웨이별 MQTT 연결 소유권을 주기적으로 조정하는 러너
 *
 * 게이트웨이마다 소유자 지정 (gatewayId % 2)
 * 평상시엔 선호 소유자가 즉시 락 획득 경쟁 없이 동작
 * 소유자가 아닌 인스턴스는 해당 게이트웨이가 일정 tick동안 계속 미등록 상태로 남아있을 때만 백업으로 채감
 *
 * 최초 부팅 시 워밍업
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayMqttConnectionReconciler {

    private static final int BACKUP_GRACE_TICKS = 3; //fixedDelay(10초) 기준 약 30초, Lock TTL

    private final DynamicMqttGatewayManager gatewayManager;
    private final GatewayMqttLockService lockService;
    private final GatewayConnectionInfoCache connectionInfoCache;
    private final GatewayGroupMappingCache groupMappingCache;
    private final GatewayRepository gatewayRepository;

    private final Map<Long, Integer> unclaimedTickCounts = new ConcurrentHashMap<>();

    @Value("${instance.slot-index:0}")
    private int instanceSlotIndex;

    @Value("${instance.total-count:1}")
    private int instanceTotalCount;

    private String instanceId;

    /**
     * {@code @Value}로 주입된 {@code instanceSlotIndex}를 바탕으로 이 인스턴스의 식별자를 조립함.
     * 필드 선언 시점에 바로 계산하면 안 되는 이유는, {@code @Value} 주입이 필드 초기화보다
     * 나중에 일어나서 그 시점엔 아직 기본값(0)만 채워진 상태이기 때문 — 모든 의존성 주입이
     * 끝난 뒤 호출이 보장되는 {@code @PostConstruct}에서 계산해야 실제 주입값이 반영됨.
     */
    @PostConstruct
    void init() {
        this.instanceId = "core-" + instanceSlotIndex;
    }

    /**
     * 10초마다 실행되며, MQTT 프로토콜 게이트웨이 전체를 대상으로 이 인스턴스가 물어야 할 연결을
     * 조정함. 게이트웨이별로 세 갈래로 처리함:
     * <ol>
     *     <li><b>이미 등록됨</b> — 내가 선호 소유자가 아닌데 원래 선호 소유자의 생존 신호가 감지되면
     *         즉시 연결을 해제하고 락을 반납함(자발적 반납). 아니면 락 TTL만 갱신함</li>
     *     <li><b>미등록 + 선호 소유자</b> — {@code gatewayId % 인스턴스 총 개수}로 계산해 내 담당이면
     *         경쟁자가 거의 없는 상태로 즉시 획득을 시도함</li>
     *     <li><b>미등록 + 선호 소유자 아님</b> — 이 게이트웨이가 {@link #BACKUP_GRACE_TICKS}번(약 30초)
     *         연속으로 계속 미등록 상태였을 때만 "원래 소유자가 죽었다"고 판단해 백업으로 대신 시도함</li>
     * </ol>
     * 최초 부팅 시의 워밍업 역할도 이 스케줄의 첫 실행이 자연스럽게 대신함(예전 {@code ApplicationRunner}
     * 방식은 다중 인스턴스 환경에서 배포로 인한 소유권 재조정을 감지 못해 이 방식으로 교체됨).
     */
    @Scheduled(fixedDelay = 10_000)
    public void reconcileGatewayConnections() {
        lockService.markAlive(instanceId);

        List<Gateway> mqttGateways = gatewayRepository.findAllByProtocolType(ProtocolType.MQTT);
        Set<Long> stillExisting = new HashSet<>(); //이번 Tick에서 실제로 DB에서 조회된 Gateway목록을 담는 HashSet

        for(Gateway gateway : mqttGateways) {
            Long gatewayId = gateway.getGatewayId();
            stillExisting.add(gatewayId);

            //manager에 gateway가 등록되어 있다면 해당 gateway의 선호 인스턴스가 누군지 확인
            if(gatewayManager.isRegistered(gatewayId)) {
                boolean isPreferredOwner = (gatewayId % instanceTotalCount) == instanceSlotIndex;

                //내가 선호 인스턴스가 아니라면
                if (!isPreferredOwner) {
                    String preferredOwnerId = "core-" + (gatewayId % instanceTotalCount);
                    //선호 인스턴스가 살아있는지 확인 후 manager에서 해제 및 lock 반납
                    if (lockService.isAlive(preferredOwnerId)) {
                        gatewayManager.unregisterGateway(gatewayId);
                        lockService.release(gatewayId, instanceId);
                        log.info("Gateway {} 원래 소유자({}) 복귀 감지, 반납", gatewayId, preferredOwnerId);
                        continue;
                    }
                }

                //선호 인스턴스라면 ttl연장 재시도 횟수 초기화
                //renew 실패(다른 인스턴스가 이미 락을 채감) 시 로컬 연결이 남의 것과 clientId가
                //충돌하지 않도록 즉시 해제 — TTL 만료 후 재획득이 늦게 감지되는 걸 방지
                boolean renewed = lockService.renew(gatewayId, instanceId);
                if (!renewed) {
                    gatewayManager.unregisterGateway(gatewayId);
                    log.warn("Gateway {} 락 갱신 실패 — 로컬 연결 강제 해제 (instance={})", gatewayId, instanceId);
                    continue;
                }
                unclaimedTickCounts.remove(gatewayId);
                continue;
            }

            //manager에 gateway가 등록되어있지 않다면 선호 인스턴스 확인
            //내가 선호 인스턴스라면 정보 캐싱 후 gateway를 manager에 등록
            //선호 인스턴스가 아니라면 tick 증가 tick이 3을 넘어간다면 선호 인스턴스가 아니더라도 gateway에 등록
            boolean isPreferredOwner = (gatewayId % instanceTotalCount) == instanceSlotIndex;
            boolean shouldAttempt = isPreferredOwner || unclaimedTickCounts.merge(gatewayId, 1, Integer::sum) >= BACKUP_GRACE_TICKS;

            if(!shouldAttempt) {
                continue;
            }

            tryRegister(gateway, isPreferredOwner);
        }
        unclaimedTickCounts.keySet().retainAll(stillExisting);
    }

    /**
     * 게이트웨이 하나에 대해 락 획득 → 실제 MQTT 연결 등록까지 시도함.
     * 락 획득에 실패하면(다른 인스턴스가 이미 보유) 조용히 리턴함 — 정상적인 경쟁 결과이므로 에러 아님.
     * 락은 얻었는데 연결 등록(브로커 접속, connection_config 파싱 등)이 실패하면, 그 락을 즉시
     * 반납함 — 안 그러면 나조차 다음 tick에 재시도를 못 하게 됨(SETNX는 기존 키를 못 덮어씀).
     *
     * @param gateway          연결을 시도할 게이트웨이
     * @param isPreferredOwner 이번 시도가 정상적인 선호 소유자 자격인지, 방치 감지로 인한 백업 시도인지
     *                          — 성공 로그에 이유를 남기기 위한 용도
     */
    private void tryRegister(Gateway gateway, boolean isPreferredOwner) {

        Long gatewayId = gateway.getGatewayId();

        if(!lockService.tryAcquire(gatewayId, instanceId)) {
            return;
        }

        try {
            MqttGatewayConnectionInfo connectionInfo = MqttGatewayConnectionInfo.from(gateway);
            groupMappingCache.put(gatewayId, gateway.getGroupId());
            connectionInfoCache.put(connectionInfo);
            gatewayManager.registerGateway(connectionInfo);
            unclaimedTickCounts.remove(gatewayId);

            String reason = isPreferredOwner ? "선호 소유자" : "백업(원 소유자 방치 감지)";
            log.info("Gateway {} MQTT 연결 소유권 획득 [{}] (instance={})", gatewayId, reason, instanceId);

        } catch (Exception e) {
            log.error("Gateway {} 연결 등록 실패, 락 반납", gatewayId, e);
            lockService.release(gatewayId, instanceId);
        }
    }

    /**
     * 정상 종료(배포로 인한 SIGTERM 등) 시, 이 인스턴스가 실제로 연결까지 맺어 물고 있던
     * 게이트웨이들의 락을 전부 즉시 반납. TTL(30초) 만료를 기다리지 않고 다른 인스턴스가
     * 바로 다음 tick(최대 10초)에 이어받을 수 있게 해서, 계획된 배포로 인한 끊김 시간을 최소화함.
     * MQTT 연결 자체(Paho 클라이언트 disconnect)는 Spring Integration의
     * {@code IntegrationFlowRegistration}이 컨텍스트 종료 시 자체적으로 처리하므로, 여기서는
     * Redis 락 반납만 책임짐.
     */
    @PreDestroy
    void releaseOwnedLocks() {
        Set<Long> owned = gatewayManager.getRegisterGatewayIds();
        owned.forEach(gatewayId -> lockService.release(gatewayId, instanceId));
        log.info("종료 - 보유 게이트웨이 락 {}건 반납 (instance={})", owned.size(), instanceId);
    }
}
