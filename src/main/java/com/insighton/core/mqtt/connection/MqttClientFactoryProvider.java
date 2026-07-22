package com.insighton.core.mqtt.connection;

import com.insighton.core.gateway.MqttGatewayConnectionInfo;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.stereotype.Component;

/**
 * {@link MqttGatewayConnectionInfo}를 받아 Paho MQTT 클라이언트 팩토리를 생성
 * 브로커 URL, 인증 정보(username/password), 재연결 옵션 등 연결 수립에 필요한 설정을 조립하는 역할만 담당
 */
@Component
public class MqttClientFactoryProvider {

    /**
     * 게이트웨이 접속 정보에 따라 MQTT 클라이언트 팩토리를 구성한다.
     * 세션을 정리하도록 설정하고, 자동 재연결은 비활성화하며, 제공된 인증 정보를 적용한다.
     *
     * @param connectionInfo 게이트웨이의 MQTT 브로커 및 인증 정보
     * @return 구성된 MQTT 클라이언트 팩토리
     */
    public MqttPahoClientFactory create(MqttGatewayConnectionInfo connectionInfo) {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();

        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(connectionInfo.brokerUrls());
        options.setCleanSession(true);
        // Spring Integration의 MqttPahoMessageDrivenChannelAdapter가 자체 recoveryInterval로 재연결을 관리하므로
        // Paho 쪽 automaticReconnect를 같이 켜두면 재연결 권한이 겹쳐 "연결 이미 진행 중" 충돌이 발생함(false 고정)
        options.setAutomaticReconnect(false);

        if (connectionInfo.username() != null) {
            options.setUserName(connectionInfo.username());
        }
        if (connectionInfo.password() != null) {
            options.setPassword(connectionInfo.password().toCharArray());
        }

        factory.setConnectionOptions(options);

        return factory;
    }
}