package com.insighton.core.domain.gateway;

import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.gateway.entity.ProtocolType;
import com.insighton.core.domain.gateway.exception.InvalidGatewayConnectionConfigException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MqttGatewayConnectionInfoTest {

    @Test
    @DisplayName("Gateway 엔티티로부터 MqttGatewayConnectionInfo 변환 성공 케이스")
    void from_validGateway_success() {
        // given
        Map<String, Object> config = new HashMap<>();
        config.put("brokerUrls", List.of("tcp://localhost:1883"));
        config.put("topics", List.of("app/+/sensor/+/event/up"));
        config.put("username", "admin");
        config.put("password", "pass");

        Gateway gateway = Gateway.builder()
                .groupsId(100L)
                .name("TestGW")
                .protocolType(ProtocolType.MQTT)
                .connectionConfig(config)
                .build();

        ReflectionTestUtils.setField(gateway, "gatewayId", 10L);

        // when
        MqttGatewayConnectionInfo info = MqttGatewayConnectionInfo.from(gateway);

        // then
        assertThat(info.gatewayId()).isEqualTo(10L);
        assertThat(info.clientId()).isEqualTo("insighton-10");
        assertThat(info.brokerUrls()).containsExactly("tcp://localhost:1883");
        assertThat(info.topics()).containsExactly("app/+/sensor/+/event/up");
        assertThat(info.username()).isEqualTo("admin");
        assertThat(info.password()).isEqualTo("pass");
    }

    @Test
    @DisplayName("brokerUrls가 없거나 비어 있으면 InvalidGatewayConnectionConfigException 발생")
    void from_missingBrokerUrls_throwsException() {
        // given
        Map<String, Object> config = new HashMap<>();
        Gateway gateway = Gateway.builder()
                .groupsId(100L)
                .name("TestGW")
                .protocolType(ProtocolType.MQTT)
                .connectionConfig(config)
                .build();

        ReflectionTestUtils.setField(gateway, "gatewayId", 10L);

        // when & then
        assertThatThrownBy(() -> MqttGatewayConnectionInfo.from(gateway))
                .isInstanceOf(InvalidGatewayConnectionConfigException.class);
    }

    @Test
    @DisplayName("equals, hashCode, toString 동작 검증")
    void equalsAndHashCodeAndToString() {
        // given
        MqttGatewayConnectionInfo info1 = new MqttGatewayConnectionInfo(
                1L, "client-1", new String[]{"url1"}, new String[]{"topic1"}, "user", "pass"
        );
        MqttGatewayConnectionInfo info2 = new MqttGatewayConnectionInfo(
                1L, "client-1", new String[]{"url1"}, new String[]{"topic1"}, "user", "pass"
        );

        // then
        assertThat(info1)
                .isEqualTo(info2)
                .hasSameHashCodeAs(info2);
        assertThat(info1.toString()).contains("client-1");
    }
}
