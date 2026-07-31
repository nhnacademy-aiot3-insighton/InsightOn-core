package com.insighton.core.mqtt.connection;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Gateway 하나당 MQTT 연결을 정확히 한 인스턴스만 할 수 있도록 조율
 * Redis 분산 Lock
 */
//TODO: LuaScript 도입 가능
// renew / release는 원자적 연산은 아님 레이스 감수해야 함 보완하고 싶다면 LuaScript 고려할 것
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayMqttLockService {

    private static final String LOCK_KEY_PREFIX = "gateway-mqtt-lock";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private static final String ALIVE_KEY_PREFIX = "instance-alive:";
    private static final Duration ALIVE_TTL = Duration.ofSeconds(15);

    private final StringRedisTemplate redisTemplate;

    /**
     * 게이트웨이 하나에 대한 MQTT 연결 소유권 락을 시도함.
     * Redis setIfAbsent(키가 없을 때만 설정)를 사용해 "확인 후 설정"이 하나의 원자적 연산으로 처리되므로,
     * 여러 인스턴스가 동시에 호출해도 정확히 하나만 성공 보장.
     * 이미 다른 인스턴스가 쥐고 있으면 조용히 실패(디버그 로그만 남김)하며, 이는 정상적인 "경쟁에서 짐" 상황.
     *
     * @param gatewayId 락을 걸 게이트웨이 PK
     * @param ownerId   락을 시도하는 인스턴스 식별자(예: {@code core-0})
     * @return 아무도 쥐고 있지 않아 내가 획득했으면 true, 이미 다른 인스턴스가 쥐고 있으면 false
     */
    public boolean tryAcquire(Long gatewayId, String ownerId) {
        Boolean acquired = redisTemplate.opsForValue()
                //원자적 연산 Key가 없으면 만들고 있으면 실패 처리
                .setIfAbsent(key(gatewayId), ownerId, LOCK_TTL);

        boolean success = Boolean.TRUE.equals(acquired);
        if (!success) {
            log.debug("Gateway {} 락 획득 실패 — 이미 다른 인스턴스가 보유 중 (요청자: {})", gatewayId, ownerId);
        }
        return success;
    }

    /**
     * 이미 쥐고 있는 락의 TTL을 연장함. 게이트웨이 연결을 계속 유지하는 인스턴스가
     * {@code @Scheduled} tick마다 호출해서, TTL이 만료되어 다른 인스턴스에게 뺏기지 않게 함.
     * 갱신 전에 Redis에 저장된 현재 소유자가 정말 나(ownerId)인지 먼저 GET으로 확인하는데,
     * 이 확인 없이 무조건 갱신하면 — 내가 TTL 만료로 이미 락을 잃은 사이 다른 인스턴스가 채간 경우
     * 남의 락 TTL을 내가 연장해주는 사고가 날 수 있기 때문임.
     *
     * @param gatewayId 갱신할 게이트웨이 PK
     * @param ownerId   갱신을 시도하는 인스턴스 식별자
     */
    public void renew(Long gatewayId, String ownerId) {
        String currentOwner = redisTemplate.opsForValue().get(key(gatewayId));

        if(ownerId.equals(currentOwner)) {
            redisTemplate.expire(key(gatewayId), LOCK_TTL);
        } else {
            log.warn("Gateway {} 락 소유자가 나({})와 다름 — 갱신 스킵 (현재 소유자: {})", gatewayId, ownerId, currentOwner);
        }
    }

    /**
     * 쥐고 있던 락을 즉시 반납함. 세 가지 상황에서 호출됨:
     * 1. 정상 종료(graceful shutdown) 시 보유 락 전체 반납 — TTL 만료를 기다리지 않고 다른 인스턴스가 바로 이어받을 수 있게 함
     * 2. 락은 획득했지만 실제 MQTT 연결 등록에 실패했을 때 롤백용 — 안 풀어주면 이 락은 나 자신도 다음 tick에 다시 못 잡는 상태(SETNX는 이미 존재하는 키는 못 만듦)로 방치됨
     * 3. 백업으로 들고 있던 게이트웨이의 원래 선호 소유자가 복귀했을 때 자발적으로 돌려줌
     * 지우기 전에 현재 소유자가 정말 나인지 먼저 확인
     * 안 그러면 이미 다른 인스턴스에게 넘어간 락을 내가 착각하고 지워버리는 사고가 날 수 있음.
     *
     * @param gatewayId 반납할 게이트웨이 PK
     * @param ownerId   반납을 시도하는 인스턴스 식별자
     */
    public void release(Long gatewayId, String ownerId) {
        String currentOwner = redisTemplate.opsForValue().get(key(gatewayId));

        if(ownerId.equals(currentOwner)) {
            redisTemplate.delete(key(gatewayId));
            log.info("Gateway {} 락 반납 (instance={})", gatewayId, ownerId);
        } else {
            log.warn("Gateway {} 반납 시도했으나 소유자가 나({})와 다름 — 스킵 (현재 소유자: {})", gatewayId, ownerId, currentOwner);
        }
    }

    /**
     * 이 인스턴스가 살아있다는 신호를 짧은 TTL(15초)로 Redis에 남김. {@code @Scheduled} tick마다 호출됨.
     * 백업으로 남의 게이트웨이를 대신 들고 있는 인스턴스가 "원래 선호 소유자가 다시 살아났는지"를
     * 판단하는 유일한 근거가 이 신호임 — 게이트웨이 락 자체엔 "누가 원래 주인이었는지" 정보가 없어서,
     * 인스턴스 생존 여부를 별도로 추적해야 자발적 반납(voluntary yield)이 가능함.
     *
     * @param instanceId 생존 신호를 남길 인스턴스 식별자
     */
    public void markAlive(String instanceId) {
        redisTemplate.opsForValue().set(ALIVE_KEY_PREFIX + instanceId, "1", ALIVE_TTL);
    }

    /**
     * 해당 인스턴스가 최근(15초 이내)에 생존 신호를 남겼는지 확인함.
     * 백업으로 게이트웨이를 들고 있는 인스턴스가, 그 게이트웨이의 원래 선호 소유자에게
     * 돌려줘야 할지 판단할 때 사용함 — true면 원래 소유자가 살아있다는 뜻이므로 반납함.
     *
     * @param instanceId 생존 여부를 확인할 인스턴스 식별자
     * @return 최근 생존 신호가 있으면 true
     */
    public boolean isAlive(String instanceId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ALIVE_KEY_PREFIX + instanceId));
    }

    /**
     * 게이트웨이 PK로 Redis 락 키 조립
     *
     * @param gatewayId 키를 만들 게이트웨이 PK
     * @return {@code gateway-mqtt-lock<gatewayId>} 형태의 Redis 키
     */
    private String key(Long gatewayId) {
        return LOCK_KEY_PREFIX + gatewayId;
    }
}
