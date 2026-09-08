package com.insighton.core.adapter.mqtt.connection;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.insighton.core.adapter.mqtt.cache.GatewayConnectionInfoCache;
import com.insighton.core.adapter.mqtt.cache.GatewayGroupMappingCache;
import com.insighton.core.domain.gateway.event.GatewayBrokerChangedEvent;
import com.insighton.core.domain.gateway.event.GatewayDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GatewayMqttEventListenerTest {

    private static final Long GATEWAY_ID = 100L;

    @Mock
    private DynamicMqttGatewayManager gatewayManager;
    @Mock
    private GatewayGroupMappingCache groupMappingCache;
    @Mock
    private GatewayConnectionInfoCache connectionInfoCache;

    private GatewayMqttEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new GatewayMqttEventListener(gatewayManager, groupMappingCache, connectionInfoCache);
    }

    @Test
    void 게이트웨이_삭제_커밋후_연결_해제와_캐시_정리가_모두_일어난다() {
        listener.onGatewayDeleted(new GatewayDeletedEvent(GATEWAY_ID));

        verify(gatewayManager).unregisterGateway(GATEWAY_ID);
        verify(groupMappingCache).evict(GATEWAY_ID);
        verify(connectionInfoCache).remove(GATEWAY_ID);
    }

    @Test
    void 연결_해제_도중_예외가_나도_캐시_정리는_보장된다() {
        doThrow(new RuntimeException("disconnect 실패")).when(gatewayManager).unregisterGateway(GATEWAY_ID);

        GatewayDeletedEvent gatewayDeletedEvent = new GatewayDeletedEvent(GATEWAY_ID);

        assertThatThrownBy(() -> listener.onGatewayDeleted(gatewayDeletedEvent))
                .isInstanceOf(RuntimeException.class);

        verify(groupMappingCache).evict(GATEWAY_ID);
        verify(connectionInfoCache).remove(GATEWAY_ID);
    }

    @Test
    void 브로커_주소_변경시_기존_연결을_해제한다() {
        listener.onGatewayBrokerChanged(new GatewayBrokerChangedEvent(GATEWAY_ID));

        verify(gatewayManager).unregisterGateway(GATEWAY_ID);
    }
}
