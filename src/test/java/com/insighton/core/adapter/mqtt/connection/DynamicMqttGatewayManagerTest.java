package com.insighton.core.adapter.mqtt.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.insighton.core.domain.gateway.MqttGatewayConnectionInfo;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistrationBuilder;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.messaging.MessageHandler;

@ExtendWith(MockitoExtension.class)
class DynamicMqttGatewayManagerTest {

    @Mock
    private MqttClientFactoryProvider clientFactoryProvider;
    @Mock
    private IntegrationFlowContext integrationFlowContext;
    @Mock
    private MessageHandler gatewayPacketHandler;
    @Mock
    private IntegrationFlowRegistrationBuilder registrationBuilder;
    @Mock
    private IntegrationFlowRegistration registration;

    private DynamicMqttGatewayManager manager;

    @BeforeEach
    void setUp() {
        manager = new DynamicMqttGatewayManager(clientFactoryProvider, integrationFlowContext, gatewayPacketHandler);
    }

    private MqttGatewayConnectionInfo connectionInfo(Long gatewayId) {
        return new MqttGatewayConnectionInfo(gatewayId, "insighton-" + gatewayId,
                new String[]{"tcp://broker:1883"}, new String[]{"topic/+"}, null, null);
    }

    private void stubSuccessfulRegistration() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(new MqttConnectOptions());
        given(clientFactoryProvider.create(any())).willReturn(factory);
        given(integrationFlowContext.registration(any())).willReturn(registrationBuilder);
        given(registrationBuilder.id(anyString())).willReturn(registrationBuilder);
        given(registrationBuilder.register()).willReturn(registration);
    }

    @Test
    void registerGateway_정상_등록하면_isRegistered가_true가_된다() {
        stubSuccessfulRegistration();

        manager.registerGateway(connectionInfo(100L));

        assertThat(manager.isRegistered(100L)).isTrue();
        assertThat(manager.getRegisterGatewayIds()).containsExactly(100L);
    }

    @Test
    void registerGateway_이미_등록되어_있으면_재등록을_스킵한다() {
        stubSuccessfulRegistration();
        manager.registerGateway(connectionInfo(100L));

        manager.registerGateway(connectionInfo(100L));

        verify(clientFactoryProvider, org.mockito.Mockito.times(1)).create(any());
    }

    @Test
    void unregisterGateway_등록된_게이트웨이는_destroy_후_목록에서_제거된다() {
        stubSuccessfulRegistration();
        manager.registerGateway(connectionInfo(100L));

        manager.unregisterGateway(100L);

        verify(registration).destroy();
        assertThat(manager.isRegistered(100L)).isFalse();
    }

    @Test
    void unregisterGateway_등록된_적_없는_게이트웨이는_아무_일도_하지_않는다() {
        manager.unregisterGateway(999L);

        verify(registration, never()).destroy();
    }

    @Test
    void unregisterGateway_destroy가_실패하면_목록에서_제거하지_않는다() {
        stubSuccessfulRegistration();
        manager.registerGateway(connectionInfo(100L));
        doThrow(new RuntimeException("destroy 실패")).when(registration).destroy();

        assertThatThrownBy(() -> manager.unregisterGateway(100L)).isInstanceOf(RuntimeException.class);

        // 이중 연결을 막기 위해 destroy 실패 시 목록에 그대로 남아있어야 함
        assertThat(manager.isRegistered(100L)).isTrue();
    }
}
