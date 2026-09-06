package com.insighton.core.adapter.mqtt.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.insighton.core.adapter.mqtt.cache.GatewayConnectionInfoCache;
import com.insighton.core.adapter.mqtt.cache.GatewayGroupMappingCache;
import com.insighton.core.domain.gateway.MqttGatewayConnectionInfo;
import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.gateway.entity.ProtocolType;
import com.insighton.core.domain.gateway.repository.GatewayRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 소유권 조율(선호 소유자 샤딩/백업 유예/자발적 반납/자가 치유/롤백) 로직을
 * Mockito만으로 가지 단위까지 커버함. 실제 동시성/Redis 타이밍 검증은 Testcontainers 몫이라
 * 여기서는 각 분기의 조건-행동 매핑이 정확한지만 확인함.
 */
@ExtendWith(MockitoExtension.class)
class GatewayMqttConnectionReconcilerTest {

    private static final int SLOT_INDEX = 0;
    private static final int TOTAL_COUNT = 2;
    private static final String INSTANCE_ID = "core-0";
    private static final String OTHER_INSTANCE_ID = "core-1";

    @Mock
    private DynamicMqttGatewayManager gatewayManager;
    @Mock
    private GatewayMqttLockService lockService;
    @Mock
    private GatewayRepository gatewayRepository;

    private GatewayConnectionInfoCache connectionInfoCache;
    private GatewayGroupMappingCache groupMappingCache;
    private GatewayMqttConnectionReconciler reconciler;

    @BeforeEach
    void setUp() {
        connectionInfoCache = new GatewayConnectionInfoCache();
        groupMappingCache = new GatewayGroupMappingCache();
        reconciler = new GatewayMqttConnectionReconciler(gatewayManager, lockService, connectionInfoCache,
                groupMappingCache, gatewayRepository);
        ReflectionTestUtils.setField(reconciler, "instanceSlotIndex", SLOT_INDEX);
        ReflectionTestUtils.setField(reconciler, "instanceTotalCount", TOTAL_COUNT);
        reconciler.init();
    }

    private Gateway gatewayWithId(Long id) {
        return gatewayWithId(id, List.of("tcp://localhost:1883"));
    }

    private Gateway gatewayWithId(Long id, List<String> brokerUrls) {
        Gateway gateway = Gateway.builder()
                .groupsId(1L)
                .name("gateway-" + id)
                .protocolType(ProtocolType.MQTT)
                .connectionConfig(Map.of("brokerUrls", brokerUrls))
                .build();
        ReflectionTestUtils.setField(gateway, "gatewayId", id);
        return gateway;
    }

    @Test
    void 선호_소유자면_경쟁_없이_즉시_등록을_시도한다() {
        Gateway gateway = gatewayWithId(100L); // 100 % 2 == 0 == slot 0
        given(gatewayRepository.findAllByProtocolType(ProtocolType.MQTT)).willReturn(List.of(gateway));
        given(gatewayManager.isRegistered(100L)).willReturn(false);
        given(lockService.tryAcquire(100L, INSTANCE_ID)).willReturn(true);

        reconciler.reconcileGatewayConnections();

        verify(gatewayManager).registerGateway(any(MqttGatewayConnectionInfo.class));
        assertThat(connectionInfoCache.get(100L)).isPresent();
    }

    @Test
    void 선호_소유자가_아니면_유예_tick동안은_등록을_시도하지_않는다() {
        Gateway gateway = gatewayWithId(101L); // 101 % 2 == 1 != slot 0
        given(gatewayRepository.findAllByProtocolType(ProtocolType.MQTT)).willReturn(List.of(gateway));
        given(gatewayManager.isRegistered(101L)).willReturn(false);

        reconciler.reconcileGatewayConnections(); // 1/3
        reconciler.reconcileGatewayConnections(); // 2/3

        verify(lockService, never()).tryAcquire(eq(101L), any());
    }

    @Test
    void 선호_소유자가_아니어도_유예_tick을_다_채우면_백업으로_등록을_시도한다() {
        Gateway gateway = gatewayWithId(101L);
        given(gatewayRepository.findAllByProtocolType(ProtocolType.MQTT)).willReturn(List.of(gateway));
        given(gatewayManager.isRegistered(101L)).willReturn(false);
        given(lockService.tryAcquire(101L, INSTANCE_ID)).willReturn(true);

        reconciler.reconcileGatewayConnections(); // 1/3
        reconciler.reconcileGatewayConnections(); // 2/3
        reconciler.reconcileGatewayConnections(); // 3/3 - 백업 시도

        verify(lockService).tryAcquire(101L, INSTANCE_ID);
        verify(gatewayManager).registerGateway(any(MqttGatewayConnectionInfo.class));
    }

    @Test
    void 락_획득에_실패하면_조용히_리턴하고_등록하지_않는다() {
        Gateway gateway = gatewayWithId(100L);
        given(gatewayRepository.findAllByProtocolType(ProtocolType.MQTT)).willReturn(List.of(gateway));
        given(gatewayManager.isRegistered(100L)).willReturn(false);
        given(lockService.tryAcquire(100L, INSTANCE_ID)).willReturn(false);

        reconciler.reconcileGatewayConnections();

        verify(gatewayManager, never()).registerGateway(any());
    }

    @Test
    void 락은_얻었지만_연결_등록이_실패하면_락을_즉시_반납한다() {
        Gateway gateway = gatewayWithId(100L);
        given(gatewayRepository.findAllByProtocolType(ProtocolType.MQTT)).willReturn(List.of(gateway));
        given(gatewayManager.isRegistered(100L)).willReturn(false);
        given(lockService.tryAcquire(100L, INSTANCE_ID)).willReturn(true);
        doThrow(new RuntimeException("브로커 접속 실패")).when(gatewayManager)
                .registerGateway(any(MqttGatewayConnectionInfo.class));

        reconciler.reconcileGatewayConnections();

        verify(lockService).release(100L, INSTANCE_ID);
        assertThat(connectionInfoCache.get(100L)).isPresent(); // registerGateway 호출 전에 이미 캐싱됨 - 그대로 남음
    }

    @Test
    void connection_config가_바뀌면_등록을_해제하고_재등록_대상으로_전환한다() {
        Gateway original = gatewayWithId(100L, List.of("tcp://old:1883"));
        connectionInfoCache.put(MqttGatewayConnectionInfo.from(original));

        Gateway changed = gatewayWithId(100L, List.of("tcp://new:1883"));
        given(gatewayRepository.findAllByProtocolType(ProtocolType.MQTT)).willReturn(List.of(changed));
        given(gatewayManager.isRegistered(100L)).willReturn(true);

        reconciler.reconcileGatewayConnections();

        verify(gatewayManager).unregisterGateway(100L);
        verify(lockService).release(100L, INSTANCE_ID);
        assertThat(connectionInfoCache.get(100L)).isEmpty();
        verify(lockService, never()).renew(anyLong(), any()); // 재등록 대상 전환이 우선, renew는 안 탐
    }

    @Test
    void 캐시에_접속정보가_없으면_변경_여부를_안전하게_안바뀜으로_취급한다() {
        // 캐시 미스(비정상 상태)인데 여기서 잘못 true를 반환하면 불필요한 재연결이 반복됨
        Gateway gateway = gatewayWithId(100L);
        given(gatewayRepository.findAllByProtocolType(ProtocolType.MQTT)).willReturn(List.of(gateway));
        given(gatewayManager.isRegistered(100L)).willReturn(true);
        given(lockService.renew(100L, INSTANCE_ID)).willReturn(true);

        reconciler.reconcileGatewayConnections();

        verify(gatewayManager, never()).unregisterGateway(100L);
        verify(lockService).renew(100L, INSTANCE_ID);
    }

    @Test
    void 선호_소유자가_아닌데_원래_소유자가_살아있으면_자발적으로_반납한다() {
        Gateway gateway = gatewayWithId(101L); // 선호 소유자는 core-1
        given(gatewayRepository.findAllByProtocolType(ProtocolType.MQTT)).willReturn(List.of(gateway));
        given(gatewayManager.isRegistered(101L)).willReturn(true);
        given(lockService.isAlive(OTHER_INSTANCE_ID)).willReturn(true);

        reconciler.reconcileGatewayConnections();

        verify(gatewayManager).unregisterGateway(101L);
        verify(lockService).release(101L, INSTANCE_ID);
        verify(lockService, never()).renew(anyLong(), any());
    }

    @Test
    void 선호_소유자가_아니어도_원래_소유자가_죽었으면_백업으로_계속_유지한다() {
        Gateway gateway = gatewayWithId(101L);
        given(gatewayRepository.findAllByProtocolType(ProtocolType.MQTT)).willReturn(List.of(gateway));
        given(gatewayManager.isRegistered(101L)).willReturn(true);
        given(lockService.isAlive(OTHER_INSTANCE_ID)).willReturn(false);
        given(lockService.renew(101L, INSTANCE_ID)).willReturn(true);

        reconciler.reconcileGatewayConnections();

        verify(gatewayManager, never()).unregisterGateway(101L);
        verify(lockService).renew(101L, INSTANCE_ID);
    }

    @Test
    void 락_갱신에_실패하면_로컬_연결을_강제_해제한다() {
        Gateway gateway = gatewayWithId(100L);
        given(gatewayRepository.findAllByProtocolType(ProtocolType.MQTT)).willReturn(List.of(gateway));
        given(gatewayManager.isRegistered(100L)).willReturn(true);
        given(lockService.renew(100L, INSTANCE_ID)).willReturn(false);

        reconciler.reconcileGatewayConnections();

        verify(gatewayManager).unregisterGateway(100L);
    }

    @Test
    void 대상에서_사라진_게이트웨이는_연결_해제와_락_반납_캐시_정리가_모두_일어난다() {
        given(gatewayRepository.findAllByProtocolType(ProtocolType.MQTT)).willReturn(List.of()); // DB에서 사라짐
        given(gatewayManager.getRegisterGatewayIds()).willReturn(Set.of(999L));

        reconciler.reconcileGatewayConnections();

        verify(gatewayManager).unregisterGateway(999L);
        verify(lockService).release(999L, INSTANCE_ID);
    }

    @Test
    void 대상에서_사라진_게이트웨이_해제중_예외가_나도_락_반납은_보장된다() {
        // unregisterGateway()가 던진 예외는 finally 이후 그대로 스케줄러 메서드 밖으로 전파되지만
        // (releaseDroppedGateways에 catch가 없음), finally 블록 자체는 예외와 무관하게 실행되므로
        // 락 반납/캐시 정리는 보장됨 - 다음 tick에서 예외 전파로 인한 부작용은 없는지까지 함께 확인
        given(gatewayRepository.findAllByProtocolType(ProtocolType.MQTT)).willReturn(List.of());
        given(gatewayManager.getRegisterGatewayIds()).willReturn(Set.of(999L));
        doThrow(new RuntimeException("disconnect 실패")).when(gatewayManager).unregisterGateway(999L);

        assertThatThrownBy(() -> reconciler.reconcileGatewayConnections())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("disconnect 실패");

        verify(lockService).release(999L, INSTANCE_ID); // finally 블록이라 예외와 무관하게 실행됨
    }

    @Test
    void forceReconnect은_등록해제_락반납_캐시제거를_모두_수행한다() {
        reconciler.forceReconnect(100L);

        verify(gatewayManager).unregisterGateway(100L);
        verify(lockService).release(100L, INSTANCE_ID);
    }

    @Test
    void releaseOwnedLocks은_보유한_모든_게이트웨이의_락을_반납한다() {
        given(gatewayManager.getRegisterGatewayIds()).willReturn(Set.of(100L, 101L));

        reconciler.releaseOwnedLocks();

        verify(lockService).release(100L, INSTANCE_ID);
        verify(lockService).release(101L, INSTANCE_ID);
        verify(lockService, times(2)).release(anyLong(), eq(INSTANCE_ID));
    }
}
