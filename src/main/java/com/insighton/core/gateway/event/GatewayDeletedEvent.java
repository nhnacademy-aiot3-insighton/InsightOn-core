package com.insighton.core.gateway.event;

/**
 * 게이트웨이 삭제 트랜잭션이 커밋된 후 MQTT 연결 해제를 트리거하기 위한 이벤트.
 * 트랜잭션 도중 롤백되면 이미 끊어진 MQTT 연결을 되돌릴 수 없기 때문에,
 * DB 삭제와 MQTT 연결 해제를 커밋 시점 기준으로 분리함.
 */
public record GatewayDeletedEvent(Long gatewayId) {
}
