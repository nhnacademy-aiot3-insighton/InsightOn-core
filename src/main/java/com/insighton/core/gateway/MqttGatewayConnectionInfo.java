package com.insighton.core.gateway;

import com.insighton.core.gateway.entity.Gateway;
import java.util.List;
import java.util.Map;

/**
 * 게이트웨이 단위 MQTT 접속 정보를 담는 값 객체.
 * {@code gateways.connection_config}(JSONB) 필드를 그대로 옮겨 담은 것으로 Gateway 엔티티 자체는 아니며,
 * MQTT 연결 수립과 게이트웨이 CRUD 양쪽이 공유해서 사용
 *
 * @param gatewayId  게이트웨이 PK
 * @param clientId   MQTT 클라이언트 ID로 사용할 값
 * @param brokerUrls 접속할 브로커 URI 목록
 * @param topics     구독할 토픽 패턴 목록. 네트워크 서버(ChirpStack/The Things Stack/AWS IoT Core 등)마다
 *                   MQTT 토픽 규격이 서로 달라서 게이트웨이별로 다르게 설정 가능해야 함
 * @param username   MQTT 인증 사용자명, 없으면 null
 * @param password   MQTT 인증 비밀번호/토큰, 없으면 null
 */
public record MqttGatewayConnectionInfo (
        Long gatewayId,
        String clientId,
        String[] brokerUrls,
        String[] topics,
        String username,
        String password
) {
    // connection_config에 topics가 명시되지 않은 게이트웨이를 위한 기본값 — ChirpStack 표준 업링크 토픽 규격.
    private static final String[] DEFAULT_TOPICS = {"application/+/device/+/event/up"};

    /**
     * {@code Gateway} 엔티티의 {@code connectionConfig}(JSONB)를 파싱해 접속 정보 객체로 변환함.
     * {@code broker_urls} 키의 배열을 브로커 URI 목록으로 뽑아내고, clientId는 다른 팀과
     * 공유하는 브로커에서 겹치지 않도록 {@code "insighton-" + gatewayId}로 조립함 — 인스턴스와
     * 무관하게 항상 같은 값이 나오는 게 의도된 동작임(다중 인스턴스 소유권 조율은
     * {@code GatewayMqttLockService}가 별도로 담당).
     * {@code topics} 키가 없으면 ChirpStack 표준 규격을 기본값으로 사용함 — 지금까지 등록된
     * 게이트웨이들이 전부 ChirpStack이라 이 키 없이도 기존과 동일하게 동작함.
     *
     * @param gateway 접속 정보를 뽑아낼 게이트웨이 엔티티
     * @return 파싱된 MQTT 접속 정보
     */
    public static MqttGatewayConnectionInfo from(Gateway gateway) {
        Map<String, Object> config = gateway.getConnectionConfig();

        String[] brokerUrl = ((List<?>) config.get("broker_urls")).stream()
                .map(String::valueOf)
                .toArray(String[]::new);

        Object topicsRaw = config.get("topics");
        String[] topics = topicsRaw == null
                ? DEFAULT_TOPICS
                : ((List<?>) topicsRaw).stream().map(String::valueOf).toArray(String[]::new);

        return new MqttGatewayConnectionInfo(
                gateway.getGatewayId(),
                "insighton-" + gateway.getGatewayId(),
                brokerUrl,
                topics,
                (String) config.get("username"),
                (String) config.get("password")
        );
    }
}