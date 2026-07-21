package com.insighton.core.gateway;

/**
 * 게이트웨이 단위 MQTT 접속 정보를 담는 값 객체.
 * {@code gateways.connection_config}(JSONB) 필드를 그대로 옮겨 담은 것으로 Gateway 엔티티 자체는 아니며,
 * MQTT 연결 수립과 게이트웨이 CRUD 양쪽이 공유해서 사용
 *
 * 구독 토픽은 게이트웨이마다 다르지 않고 ChirpStack 표준 규격으로 고정이라 여기 담지 않음
 * ({@link com.insighton.core.mqtt.connection.DynamicMqttGatewayManager}의 상수 참고).
 *
 * @param gatewayId  게이트웨이 PK
 * @param clientId   MQTT 클라이언트 ID로 사용할 값
 * @param brokerUrls 접속할 브로커 URI 목록
 * @param username   MQTT 인증 사용자명, 없으면 null
 * @param password   MQTT 인증 비밀번호/토큰, 없으면 null
 */
public record MqttGatewayConnectionInfo (
        Long gatewayId,
        String clientId,
        String[] brokerUrls,
        String username,
        String password
) {
}