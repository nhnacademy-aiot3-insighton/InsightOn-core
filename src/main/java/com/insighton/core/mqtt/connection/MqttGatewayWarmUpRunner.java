package com.insighton.core.mqtt.connection;

import com.insighton.core.gateway.MqttGatewayConnectionInfo;
import com.insighton.core.gateway.entity.Gateway;
import com.insighton.core.gateway.entity.ProtocolType;
import com.insighton.core.gateway.repository.GatewayRepository;
import com.insighton.core.mqtt.cache.GatewayGroupMappingCache;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 서비스 기동 시점에 등록된 MQTT 게이트웨이 전체를 일괄 조회해 클라이언트를 미리 띄워두는 Warm-up 러너.
 * 게이트웨이 하나의 등록 실패가 나머지 게이트웨이 기동을 막지 않도록 개별적으로 예외를 격리함.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttGatewayWarmUpRunner implements ApplicationRunner {
    private final DynamicMqttGatewayManager gatewayManager;
    private final GatewayConnectionInfoCache connectionInfoCache;
    private final GatewayGroupMappingCache groupMappingCache;
    private final GatewayRepository gatewayRepository;

    /**
     * 애플리케이션 기동 완료 직후 실행되며, MQTT 프로토콜 게이트웨이 목록을 접속정보 캐시에 채우고
     * {@link DynamicMqttGatewayManager}로 각각의 MQTT 클라이언트를 등록함.
     *
     * @param args 애플리케이션 실행 인자
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {

        groupMappingCache.put(1L, 1L); // TEMP: gatewayId=1 -> groupId=1 매핑, 실제 캐시 채우기 로직 대신

        List<Gateway> mqttGateways = gatewayRepository.findAllByProtocolType(ProtocolType.MQTT);

        log.info("MQTT Gateway Warm-up 시작 - 대상 {}건", mqttGateways.size());

        for (Gateway gateway : mqttGateways) {
            try {
                MqttGatewayConnectionInfo connectionInfo = MqttGatewayConnectionInfo.from(gateway);
                groupMappingCache.put(gateway.getGatewayId(), gateway.getGroupsId());
                connectionInfoCache.put(connectionInfo);
                gatewayManager.registerGateway(connectionInfo);
            } catch (Exception e) {
                log.error("Gateway {} Warm-up 등록 실패", gateway.getGatewayId(), e);
            }
        }
    }
}