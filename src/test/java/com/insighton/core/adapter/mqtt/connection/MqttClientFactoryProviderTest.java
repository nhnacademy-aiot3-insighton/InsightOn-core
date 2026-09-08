package com.insighton.core.adapter.mqtt.connection;

import static org.assertj.core.api.Assertions.assertThat;

import com.insighton.core.domain.gateway.MqttGatewayConnectionInfo;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.jupiter.api.Test;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;

class MqttClientFactoryProviderTest {

    private final MqttClientFactoryProvider provider = new MqttClientFactoryProvider();

    private MqttGatewayConnectionInfo connectionInfo(String username, String password) {
        return new MqttGatewayConnectionInfo(1L, "insighton-1",
                new String[]{"tcp://broker:1883"}, new String[]{"topic/+"}, username, password);
    }

    @Test
    void persistent_session과_수동_재연결_옵션이_고정으로_설정된다() {
        MqttPahoClientFactory factory = provider.create(connectionInfo(null, null));
        MqttConnectOptions options = factory.getConnectionOptions();

        assertThat(options.isCleanSession()).isFalse();
        assertThat(options.isAutomaticReconnect()).isFalse();
        assertThat(options.getServerURIs()).containsExactly("tcp://broker:1883");
    }

    @Test
    void username_password가_있으면_인증_옵션에_채워진다() {
        MqttPahoClientFactory factory = provider.create(connectionInfo("user1", "pass1"));
        MqttConnectOptions options = factory.getConnectionOptions();

        assertThat(options.getUserName()).isEqualTo("user1");
        assertThat(options.getPassword()).isEqualTo("pass1".toCharArray());
    }

    @Test
    void username_password가_없으면_인증_옵션은_비워둔다() {
        MqttPahoClientFactory factory = provider.create(connectionInfo(null, null));
        MqttConnectOptions options = factory.getConnectionOptions();

        assertThat(options.getUserName()).isNull();
        assertThat(options.getPassword()).isNull();
    }
}
