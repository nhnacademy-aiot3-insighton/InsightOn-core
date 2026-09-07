package com.insighton.core.domain.gateway;

import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.gateway.exception.InvalidGatewayConnectionConfigException;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private static final String[] DEFAULT_TOPICS = {"application/+/sensor/+/event/up"};

    /**
     * {@code Gateway} 엔티티의 {@code connectionConfig}(JSONB)를 파싱해 접속 정보 객체로 변환함.
     * {@code brokerUrls} 키의 배열을 브로커 URI 목록으로 뽑아내고, clientId는 다른 팀과
     * 공유하는 브로커에서 겹치지 않도록 {@code "insighton-" + gatewayId}로 조립함 — 인스턴스와
     * 무관하게 항상 같은 값이 나오는 게 의도된 동작임(다중 인스턴스 소유권 조율은
     * {@code GatewayMqttLockService}가 별도로 담당).
     * {@code topics} 키가 없으면 ChirpStack 표준 규격을 기본값으로 사용함 — 지금까지 등록된
     * 게이트웨이들이 전부 ChirpStack이라 이 키 없이도 기존과 동일하게 동작함.
     * {@code brokerUrls}가 없거나 형식이 안 맞으면 {@link InvalidGatewayConnectionConfigException}을
     * 던짐 — {@code GatewayServiceImpl.validateConnectionConfig()}가 REST 생성/수정 시점에 걸러내지만,
     * 그 검증이 생기기 전에 저장된 데이터나 DB 직접 수정으로 형식이 깨진 경우를 대비한 방어임.
     *
     * @param gateway 접속 정보를 뽑아낼 게이트웨이 엔티티
     * @return 파싱된 MQTT 접속 정보
     */
    public static MqttGatewayConnectionInfo from(Gateway gateway) {
        Map<String, Object> config = gateway.getConnectionConfig();

        if (!(config.get("brokerUrls") instanceof List<?> brokerUrlsRaw) || brokerUrlsRaw.isEmpty()) {
            throw new InvalidGatewayConnectionConfigException(
                    "게이트웨이 " + gateway.getGatewayId() + "의 connection_config에 brokerUrls가 없습니다.");
        }
        String[] brokerUrl = brokerUrlsRaw.stream()
                .map(String::valueOf)
                .toArray(String[]::new);

        Object topicsRaw = config.get("topics");
        String[] topics = topicsRaw == null
                ? DEFAULT_TOPICS
                : ((List<?>) topicsRaw).stream().map(String::valueOf).toArray(String[]::new);

        return new MqttGatewayConnectionInfo(
                gateway.getGatewayId(),
                "insightonsdf-" + gateway.getGatewayId(),
                brokerUrl,
                topics,
                (String) config.get("username"),
                (String) config.get("password")
        );
    }

    /**
     * record 기본 equals는 배열 필드({@code brokerUrls}, {@code topics})를 참조로 비교하므로,
     * 내용이 같아도 인스턴스가 다르면 항상 false가 나옴 — {@code .from()}이 매 호출마다 새 배열을
     * 만들기 때문에 내용 기준 비교가 되도록 직접 오버라이드함.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MqttGatewayConnectionInfo(
                Long id, String clientId1, String[] urls, String[] topics1, String username1, String password1
        ))) {
            return false;
        }
        return Objects.equals(gatewayId, id)
                && Objects.equals(clientId, clientId1)
                && Arrays.equals(brokerUrls, urls)
                && Arrays.equals(topics, topics1)
                && Objects.equals(username, username1)
                && Objects.equals(password, password1);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(gatewayId, clientId, username, password);
        result = 31 * result + Arrays.hashCode(brokerUrls);
        result = 31 * result + Arrays.hashCode(topics);
        return result;
    }

    @Override
    public @NonNull String toString() {
        return "MqttGatewayConnectionInfo[" +
                "gatewayId=" + gatewayId +
                ", clientId=" + clientId +
                ", brokerUrls=" + Arrays.toString(brokerUrls) +
                ", topics=" + Arrays.toString(topics) +
                ", username=" + username +
                ", password=" + password +
                ']';
    }
}