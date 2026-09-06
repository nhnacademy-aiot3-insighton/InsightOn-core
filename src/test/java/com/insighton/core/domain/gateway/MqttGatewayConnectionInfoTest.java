package com.insighton.core.domain.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.gateway.entity.ProtocolType;
import com.insighton.core.domain.gateway.exception.InvalidGatewayConnectionConfigException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MqttGatewayConnectionInfoTest {

    private Gateway gatewayWith(Map<String, Object> connectionConfig) {
        Gateway gateway = Gateway.builder()
                .groupsId(1L)
                .name("gateway-1")
                .protocolType(ProtocolType.MQTT)
                .connectionConfig(connectionConfig)
                .build();
        ReflectionTestUtils.setField(gateway, "gatewayId", 42L);
        return gateway;
    }

    @Test
    void connection_config를_그대로_옮겨_담고_clientId는_gatewayId_기반으로_고정_조립된다() {
        Gateway gateway = gatewayWith(Map.of(
                "brokerUrls", List.of("tcp://broker1:1883", "tcp://broker2:1883"),
                "topics", List.of("custom/topic/+"),
                "username", "user1",
                "password", "pass1"
        ));

        MqttGatewayConnectionInfo info = MqttGatewayConnectionInfo.from(gateway);

        assertThat(info.gatewayId()).isEqualTo(42L);
        assertThat(info.clientId()).isEqualTo("insighton-42");
        assertThat(info.brokerUrls()).containsExactly("tcp://broker1:1883", "tcp://broker2:1883");
        assertThat(info.topics()).containsExactly("custom/topic/+");
        assertThat(info.username()).isEqualTo("user1");
        assertThat(info.password()).isEqualTo("pass1");
    }

    @Test
    void topics가_없으면_ChirpStack_표준_토픽을_기본값으로_사용한다() {
        Gateway gateway = gatewayWith(Map.of(
                "brokerUrls", List.of("tcp://broker1:1883")
        ));

        MqttGatewayConnectionInfo info = MqttGatewayConnectionInfo.from(gateway);

        assertThat(info.topics()).containsExactly("application/+/sensor/+/event/up");
    }

    @Test
    void username_password가_없으면_null로_채워진다() {
        Gateway gateway = gatewayWith(Map.of(
                "brokerUrls", List.of("tcp://broker1:1883")
        ));

        MqttGatewayConnectionInfo info = MqttGatewayConnectionInfo.from(gateway);

        assertThat(info.username()).isNull();
        assertThat(info.password()).isNull();
    }

    @Test
    void brokerUrls_키가_없으면_예외를_던진다() {
        Gateway gateway = gatewayWith(Map.of("topics", List.of("some/topic")));

        assertThatThrownBy(() -> MqttGatewayConnectionInfo.from(gateway))
                .isInstanceOf(InvalidGatewayConnectionConfigException.class);
    }

    @Test
    void brokerUrls가_빈_리스트여도_예외를_던진다() {
        Gateway gateway = gatewayWith(Map.of("brokerUrls", List.of()));

        assertThatThrownBy(() -> MqttGatewayConnectionInfo.from(gateway))
                .isInstanceOf(InvalidGatewayConnectionConfigException.class);
    }

    @Test
    void brokerUrls가_리스트가_아니면_예외를_던진다() {
        Gateway gateway = gatewayWith(Map.of("brokerUrls", "tcp://broker1:1883"));

        assertThatThrownBy(() -> MqttGatewayConnectionInfo.from(gateway))
                .isInstanceOf(InvalidGatewayConnectionConfigException.class);
    }
}
