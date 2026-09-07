package com.insighton.core.adapter.mqtt.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.gateway.entity.GatewayStatus;
import com.insighton.core.domain.gateway.entity.ProtocolType;
import com.insighton.core.domain.gateway.event.GatewayStatusChangedEvent;
import com.insighton.core.domain.gateway.repository.GatewayRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 하트비트 기반 FAULT 판정 + 자동 재연결 트리거 + 자동 복구 로직 검증.
 * 락 상태와 무관하게 "실제 패킷이 끊겼는가"만으로 FAULT를 판단하는 게 핵심이라
 * Redis/DB 없이 하트비트 스냅샷만 조작하는 순수 Mockito 테스트로 충분함.
 */
@ExtendWith(MockitoExtension.class)
class GatewayHealthMonitorTest {

    private static final long FAULT_THRESHOLD_SECONDS = 1800L;

    @Mock
    private DynamicMqttGatewayManager gatewayManager;
    @Mock
    private GatewayHeartbeatTracker heartbeatTracker;
    @Mock
    private GatewayRepository gatewayRepository;
    @Mock
    private GatewayMqttConnectionReconciler reconciler;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private GatewayHealthMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new GatewayHealthMonitor(gatewayManager, heartbeatTracker, gatewayRepository, reconciler,
                eventPublisher);
        ReflectionTestUtils.setField(monitor, "faultThresholdSeconds", FAULT_THRESHOLD_SECONDS);
    }

    private Gateway gatewayWithId(Long id) {
        Gateway gateway = Gateway.builder()
                .groupsId(1L)
                .name("gateway-" + id)
                .protocolType(ProtocolType.MQTT)
                .connectionConfig(Map.of("brokerUrls", List.of("tcp://localhost:1883")))
                .build();
        ReflectionTestUtils.setField(gateway, "gatewayId", id);
        return gateway;
    }

    @Test
    void 보유_게이트웨이가_없으면_아무_처리도_하지_않는다() {
        given(gatewayManager.getRegisterGatewayIds()).willReturn(Set.of());

        monitor.checkGatewayHealth();

        verify(gatewayRepository, never()).findAllById(any());
    }

    @Test
    void 하트비트_기록이_없는_게이트웨이는_판단을_보류한다() {
        Gateway gateway = gatewayWithId(1L);
        given(gatewayManager.getRegisterGatewayIds()).willReturn(Set.of(1L));
        given(gatewayRepository.findAllById(Set.of(1L))).willReturn(List.of(gateway));
        given(heartbeatTracker.snapshot()).willReturn(Map.of()); // 아직 패킷 못받음

        monitor.checkGatewayHealth();

        assertThat(gateway.getStatus()).isEqualTo(GatewayStatus.ACTIVE); // 그대로, 판단 안 함
        verify(reconciler, never()).forceReconnect(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void 임계시간_이상_무응답이면_FAULT로_전환하고_강제_재연결한다() {
        Gateway gateway = gatewayWithId(1L);
        Instant longAgo = Instant.now().minusSeconds(FAULT_THRESHOLD_SECONDS + 60);
        given(gatewayManager.getRegisterGatewayIds()).willReturn(Set.of(1L));
        given(gatewayRepository.findAllById(Set.of(1L))).willReturn(List.of(gateway));
        given(heartbeatTracker.snapshot()).willReturn(Map.of(1L, longAgo));

        monitor.checkGatewayHealth();

        assertThat(gateway.getStatus()).isEqualTo(GatewayStatus.FAULT);
        verify(reconciler).forceReconnect(1L);
        verify(eventPublisher).publishEvent(any(GatewayStatusChangedEvent.class));
    }

    @Test
    void 이미_FAULT인_게이트웨이는_강제_재연결을_반복_트리거하지_않는다() {
        Gateway gateway = gatewayWithId(1L);
        gateway.markFault();
        Instant longAgo = Instant.now().minusSeconds(FAULT_THRESHOLD_SECONDS + 60);
        given(gatewayManager.getRegisterGatewayIds()).willReturn(Set.of(1L));
        given(gatewayRepository.findAllById(Set.of(1L))).willReturn(List.of(gateway));
        given(heartbeatTracker.snapshot()).willReturn(Map.of(1L, longAgo));

        monitor.checkGatewayHealth();

        verify(reconciler, never()).forceReconnect(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void 정상_범위_하트비트는_상태를_갱신하고_이벤트를_발행하지_않는다() {
        Gateway gateway = gatewayWithId(1L);
        Instant justNow = Instant.now();
        given(gatewayManager.getRegisterGatewayIds()).willReturn(Set.of(1L));
        given(gatewayRepository.findAllById(Set.of(1L))).willReturn(List.of(gateway));
        given(heartbeatTracker.snapshot()).willReturn(Map.of(1L, justNow));

        monitor.checkGatewayHealth();

        assertThat(gateway.getStatus()).isEqualTo(GatewayStatus.ACTIVE);
        assertThat(gateway.getLastHeartbeatAt()).isNotNull();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void FAULT였던_게이트웨이가_하트비트가_재개되면_ACTIVE로_복구하고_이벤트를_발행한다() {
        Gateway gateway = gatewayWithId(1L);
        gateway.markFault();
        OffsetDateTime oldHeartbeat = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
        ReflectionTestUtils.setField(gateway, "lastHeartbeatAt", oldHeartbeat);
        Instant justNow = Instant.now();
        given(gatewayManager.getRegisterGatewayIds()).willReturn(Set.of(1L));
        given(gatewayRepository.findAllById(Set.of(1L))).willReturn(List.of(gateway));
        given(heartbeatTracker.snapshot()).willReturn(Map.of(1L, justNow));

        monitor.checkGatewayHealth();

        assertThat(gateway.getStatus()).isEqualTo(GatewayStatus.ACTIVE);
        verify(eventPublisher).publishEvent(any(GatewayStatusChangedEvent.class));
        verify(reconciler, never()).forceReconnect(any());
    }
}
