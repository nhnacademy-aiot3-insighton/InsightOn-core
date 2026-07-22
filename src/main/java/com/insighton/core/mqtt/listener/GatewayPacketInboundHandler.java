package com.insighton.core.mqtt.listener;

import com.insighton.core.influx.TelemetryInfluxWriter;
import com.insighton.core.influx.dto.DynamicTelemetryMeasurement;
import com.insighton.core.mqtt.cache.DeviceLookupCacheService;
import com.insighton.core.mqtt.cache.GatewayGroupMappingCache;
import com.insighton.core.mqtt.cache.dto.DeviceCacheEntry;
import com.insighton.core.mqtt.connection.DynamicMqttGatewayManager;
import com.insighton.core.mqtt.connection.GatewayHeartbeatTracker;
import com.insighton.core.mqtt.listener.dto.CleanTelemetryPacket;
import com.insighton.core.rabbitmq.TelemetryPublisher;
import com.insighton.core.rabbitmq.dto.TelemetryEventMessage;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

/**
 * 모든 게이트웨이의 MQTT 인바운드 flow가 공유하는 패킷 진입점.
 * 현재는 본문이 비어 있으며, 추후 메타데이터 필터링 → devEui 캐시 조회 → InfluxDB/RabbitMQ 디스패치
 * 로직이 여기 채워질 예정
 */
@Component("gatewayPacketHandler")
@RequiredArgsConstructor
@Slf4j
public class GatewayPacketInboundHandler implements MessageHandler {

    private final MqttPayloadParser payloadParser;
    private final DeviceLookupCacheService deviceLookupCacheService;
    private final GatewayGroupMappingCache groupMappingCache;
    private final GatewayHeartbeatTracker heartbeatTracker;
    private final TelemetryPublisher telemetryPublisher;
    private final TelemetryInfluxWriter influxWriter;

    /**
     * MQTT 수신 패킷을 파싱하고 디바이스 컨텍스트에 따라 텔레메트리를 처리한다.
     * 파싱에 실패하거나 필수 컨텍스트를 확인할 수 없는 패킷은 삭제하며, 미등록 디바이스는
     * InfluxDB에만 적재하고 등록된 디바이스의 텔레메트리만 RabbitMQ로 발행한다.
     *
     * @param message 수신한 MQTT 메시지
     */
    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        Long gatewaysId = message.getHeaders().get(DynamicMqttGatewayManager.GATEWAY_ID_HEADER, Long.class);

        if (gatewaysId != null) {
            heartbeatTracker.recordHeartbeat(gatewaysId);
        } else {
            log.warn("gatewayId 헤더 없음 — DynamicMqttGatewayManager의 enrichHeaders 누락 의심 (topic = {})",
                    message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC));
        }

        Optional<CleanTelemetryPacket> optionalPacket = payloadParser.parse(message.getPayload());

        if(optionalPacket.isEmpty()) {
            log.warn("MQTT 페이로드 파싱 실패, 패킷 드롭 (topic = {})", message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC));
            return;
        }
        CleanTelemetryPacket packet = optionalPacket.get();
        String deviceEui = packet.devEui();

        Optional<DeviceCacheEntry> deviceCacheEntry = deviceLookupCacheService.lookup(deviceEui);

        Long locationId;
        Long groupId;
        Long resolvedDeviceId = null;

        if (deviceCacheEntry.isPresent()) {
            DeviceCacheEntry device = deviceCacheEntry.get();
            locationId = device.locationId();

            if (locationId == null) {
                log.debug("공간 미배치 기기 패킷 드롭 (devEui = {}, deviceId = {})", deviceEui, device.deviceId());
                return;
            }

            Optional<Long> optionalGroupId = groupMappingCache.get(device.gatewayId());

            if (optionalGroupId.isEmpty()) {
                log.warn("게이트웨이 {}의 group 매핑이 캐시에 없어 패킷 드롭 (devEui = {})", device.gatewayId(), deviceEui);
                return;
            }

            groupId = optionalGroupId.get();
            resolvedDeviceId = device.deviceId();
        } else {
            // TEMP: Issue04(Auto-Provisioning) 전까지 InfluxDB 적재 파이프라인 테스트용 — 실제 서비스 로직 아님, Issue04 완료 시 제거
            //TODO: device / device_attributes 생성
            // deviceLookupCacheService.populate() 호출 캐시 채우기
            log.info("미등록 기기 패킷 수신, TEMP 플레이스홀더로 InfluxDB만 적재 devEui = {}", deviceEui);
            locationId = 0L;
            groupId = 0L;
        }

        Instant time = Instant.parse(packet.time());
        Map<String, Object> fields = packet.object();


        DynamicTelemetryMeasurement telemetryMeasurement = new DynamicTelemetryMeasurement(
                time,
                String.valueOf(groupId),
                String.valueOf(locationId),
                deviceEui,
                packet.deviceName(),
                fields
        );

        //InfluxDB 적재
        influxWriter.write(telemetryMeasurement);

        if (resolvedDeviceId == null) {
            // 미등록 기기는 devices_id가 없어 Rule Engine 매칭이 의미 없으므로 RabbitMQ 발행은 건너뜀
            return;
        }

        // RabbitMQ 논블로킹 디스패처로 telemetryMeasurement 전달
        TelemetryEventMessage telemetryEventMessage = new TelemetryEventMessage(
                groupId,
                locationId,
                resolvedDeviceId,
                fields,
                time
        );

        telemetryPublisher.publish(telemetryEventMessage);
    }
}