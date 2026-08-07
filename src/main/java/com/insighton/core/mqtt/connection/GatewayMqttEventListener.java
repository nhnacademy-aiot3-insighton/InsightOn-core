package com.insighton.core.mqtt.connection;

import com.insighton.core.gateway.event.GatewayBrokerChangedEvent;
import com.insighton.core.gateway.event.GatewayDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 게이트웨이 삭제 트랜잭션이 실제로 커밋된 후에만 MQTT 연결을 해제함.
 * 커밋 전에 해제하면, 이후 같은 트랜잭션에서 예외가 나 롤백되더라도
 * 이미 끊어진 MQTT 클라이언트는 복구되지 않아 DB와 상태가 어긋나게 됨.
 */
@Component
@RequiredArgsConstructor
public class GatewayMqttEventListener {

    private final DynamicMqttGatewayManager gatewayManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGatewayDeleted(GatewayDeletedEvent event) {
        gatewayManager.unregisterGateway(event.gatewayId());
    }

    /**
     * 브로커 주소가 바뀐 게이트웨이의 기존 MQTT 연결 해제
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGatewayBrokerChanged(GatewayBrokerChangedEvent event) {
        gatewayManager.unregisterGateway(event.gatewayId());
    }
}
