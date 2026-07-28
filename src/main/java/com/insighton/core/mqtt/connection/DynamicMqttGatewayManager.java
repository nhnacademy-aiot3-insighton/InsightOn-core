package com.insighton.core.mqtt.connection;

import com.insighton.core.gateway.MqttGatewayConnectionInfo;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

/**
 * 게이트웨이별 MQTT 인바운드 클라이언트를 런타임에 동적으로 등록/해제하는 매니저.
 * 게이트웨이 목록이 컴파일 타임이 아니라 DB 조회 결과로 정해지기 때문에 {@code @Bean} 정적 선언 대신
 * {@link IntegrationFlowContext}로 flow를 즉석에서 등록하는 방식을 사용함.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicMqttGatewayManager {

    // ChirpStack 표준 업링크 토픽 규격 — 그룹마다 브로커는 달라도 토픽 형식은 동일해서 상수로 고정.
    private static final String[] SUBSCRIBE_TOPICS = {"application/+/device/+/event/up"};

    // 이 flow를 타고 들어오는 모든 메시지에 소속 게이트웨이 ID를 표식으로 남기는 헤더 키.
    // devEui 캐시 조회 성공 여부와 무관하게 GatewayPacketInboundHandler가 하트비트를 찍을 수 있도록 함.
    public static final String GATEWAY_ID_HEADER = "gatewayId";

    private final MqttClientFactoryProvider clientFactoryProvider;
    private final IntegrationFlowContext integrationFlowContext;
    private final MessageHandler gatewayPacketHandler;
    private final Map<Long, IntegrationFlowRegistration> registrations = new ConcurrentHashMap<>();

    /**
     * 게이트웨이 하나에 대한 MQTT 인바운드 어댑터를 만들고 flow로 등록함.
     * 이미 등록된 게이트웨이면 재등록 없이 스킵함. 등록된 flow는 {@link #unregisterGateway(Long)}로 해제 가능함.
     *
     * @param connectionInfo 등록할 게이트웨이의 접속 정보
     */
    public void registerGateway(MqttGatewayConnectionInfo connectionInfo) {
        if (registrations.containsKey(connectionInfo.gatewayId())) {
            log.warn("Gateway {} 이미 등록되어 있어 스킵", connectionInfo.gatewayId());
            return;
        }

        MqttPahoClientFactory clientFactory = clientFactoryProvider.create(connectionInfo);

        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                connectionInfo.clientId(),
                clientFactory,
                SUBSCRIBE_TOPICS
        );

        adapter.setCompletionTimeout(30000); // Paho 내부 connectionTimeout(기본 30초)보다 짧으면 SI가 먼저 재시도를 걸어 "연결 이미 진행 중" 충돌 발생
        adapter.setQos(1);

        IntegrationFlow flow = IntegrationFlow
                .from(adapter)
                .enrichHeaders(h -> h.header(GATEWAY_ID_HEADER, connectionInfo.gatewayId()))
                .handle(gatewayPacketHandler)
                .get();

        IntegrationFlowRegistration registration = integrationFlowContext
                .registration(flow)
                .id("mqtt-gateway-" + connectionInfo.gatewayId())
                .register();

        registrations.put(connectionInfo.gatewayId(), registration);
        log.info("Gateway {} MQTT 클라이언트 등록 완료 ( topics = {} )", connectionInfo.gatewayId(), SUBSCRIBE_TOPICS);
    }

    /**
     * 등록돼 있던 게이트웨이의 MQTT flow를 해제하고 내부 등록 목록에서 제거
     * 등록된 적 없는 게이트웨이 ID가 들어오면 아무 동작도 하지 않음.
     *
     * @param gatewayId 해제할 게이트웨이 PK
     */
    public void unregisterGateway(Long gatewayId) {
        IntegrationFlowRegistration registration = registrations.remove(gatewayId);

        if (registration != null) {
            registration.destroy();
            log.info("Gateway {} MQTT 클라이언트 해제", gatewayId);
        }
    }
}