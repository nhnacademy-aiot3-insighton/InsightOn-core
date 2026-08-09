package com.insighton.core.adapter.mqtt.connection;

import com.insighton.core.adapter.mqtt.cache.GatewayConnectionInfoCache;
import com.insighton.core.adapter.mqtt.cache.GatewayGroupMappingCache;
import com.insighton.core.domain.gateway.event.GatewayBrokerChangedEvent;
import com.insighton.core.domain.gateway.event.GatewayDeletedEvent;
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
    private final GatewayGroupMappingCache groupMappingCache;
    private final GatewayConnectionInfoCache connectionInfoCache;

    /**
     * 연결 해제와 함께 두 캐시도 여기서 직접 비움. unregisterGateway()를 먼저 호출하면
     * Manager의 등록 목록에서 이 gatewayId가 즉시 빠지는데, Reconciler.releaseDroppedGateways()는
     * 바로 그 등록 목록을 순회하며 캐시를 비우기 때문에 — 여기서 먼저 지워버리면 다음 tick의
     * releaseDroppedGateways()는 이 gatewayId를 영영 보지 못해 캐시 정리가 누락됨.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGatewayDeleted(GatewayDeletedEvent event) {
        gatewayManager.unregisterGateway(event.gatewayId());
        groupMappingCache.evict(event.gatewayId());
        connectionInfoCache.remove(event.gatewayId());
    }

    /**
     * 브로커 주소가 바뀐 게이트웨이의 기존 MQTT 연결 해제.
     * 게이트웨이 자체는 DB에 남아있어 다음 Reconciler tick에 재등록되면서
     * groupMappingCache/connectionInfoCache도 새 값으로 자연스럽게 덮어써지므로,
     * 여기서 캐시를 미리 비울 필요는 없음.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGatewayBrokerChanged(GatewayBrokerChangedEvent event) {
        gatewayManager.unregisterGateway(event.gatewayId());
    }
}
