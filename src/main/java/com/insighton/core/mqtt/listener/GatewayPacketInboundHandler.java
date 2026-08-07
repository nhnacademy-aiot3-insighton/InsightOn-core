package com.insighton.core.mqtt.listener;

import com.insighton.core.influx.TelemetryInfluxWriter;
import com.insighton.core.influx.dto.DynamicTelemetryMeasurement;
import com.insighton.core.mqtt.cache.SensorLookupCacheService;
import com.insighton.core.mqtt.cache.GatewayGroupMappingCache;
import com.insighton.core.mqtt.cache.dto.SensorCacheEntry;
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
 * {@code DynamicMqttGatewayManager}가 게이트웨이마다 만든 어댑터/flow가 전부 이 핸들러 하나로
 * 모이며, 하트비트 기록 → 페이로드 파싱 → devEui 캐시 조회 → InfluxDB/RabbitMQ 디스패치까지
 * 패킷 하나에 대한 처리 전체를 담당함.
 */
@Component("gatewayPacketHandler")
@RequiredArgsConstructor
@Slf4j
public class GatewayPacketInboundHandler implements MessageHandler {

    private final MqttPayloadParser payloadParser;
    private final SensorLookupCacheService sensorLookupCacheService;
    private final GatewayGroupMappingCache groupMappingCache;
    private final GatewayHeartbeatTracker heartbeatTracker;
    private final TelemetryPublisher telemetryPublisher;
    private final TelemetryInfluxWriter influxWriter;

    /**
     * MQTT로부터 수신한 메시지 하나를 처리함. 순서대로:
     * <ol>
     *     <li>{@code gatewayId} 헤더로 하트비트 기록 — devEui 조회 성공 여부와 무관하게
     *         "이 게이트웨이로부터 뭔가 도착했다"는 사실 자체를 우선 기록함</li>
     *     <li>{@link MqttPayloadParser}로 페이로드를 정제된 {@link CleanTelemetryPacket}으로 파싱,
     *         실패하면 드롭</li>
     *     <li>devEui로 {@link SensorLookupCacheService} 조회 — 등록된 기기면 locationId/groupId/
     *         sensorId를 확정하고(위치 미배치면 드롭), 미등록이면 TEMP 플레이스홀더로 InfluxDB
     *         파이프라인만 태움(Auto-Provisioning 미구현 상태의 임시 처리)</li>
     *     <li>InfluxDB에 적재(항상) — {@link TelemetryInfluxWriter}, 비동기</li>
     *     <li>등록된 기기인 경우에만 RabbitMQ로 발행 — {@link TelemetryPublisher}, 비동기.
     *         미등록 기기는 sensorId가 없어 Rule Engine 매칭이 의미 없으므로 건너뜀</li>
     * </ol>
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
        String sensorEui = packet.sensorEui();

        Optional<SensorCacheEntry> sensorCacheEntry = sensorLookupCacheService.lookup(sensorEui);

        Long locationId;
        Long groupId;
        Long resolvedSensorId = null;

        if (sensorCacheEntry.isPresent()) {
            SensorCacheEntry sensor = sensorCacheEntry.get();
            locationId = sensor.locationId();

            if (locationId == null) {
                log.debug("공간 미배치 기기 패킷 드롭 (devEui = {}, sensorId = {})", sensorEui, sensor.sensorId());
                return;
            }

            Optional<Long> optionalGroupId = groupMappingCache.get(sensor.gatewayId());

            if (optionalGroupId.isEmpty()) {
                log.warn("게이트웨이 {}의 group 매핑이 캐시에 없어 패킷 드롭 (devEui = {})", sensor.gatewayId(), sensorEui);
                return;
            }

            groupId = optionalGroupId.get();
            resolvedSensorId = sensor.sensorId();
        } else {
            // TEMP: Issue04(Auto-Provisioning) 전까지 InfluxDB 적재 파이프라인 테스트용 — 실제 서비스 로직 아님, Issue04 완료 시 제거
            //TODO: sensor / sensor_attributes 생성
            // sensorLookupCacheService.populate() 호출 캐시 채우기
            log.info("미등록 기기 패킷 수신, TEMP 플레이스홀더로 InfluxDB만 적재 devEui = {}", sensorEui);
            locationId = 0L;
            groupId = 0L;
        }

        Instant time = Instant.parse(packet.time());
        Map<String, Object> fields = packet.object();


        DynamicTelemetryMeasurement telemetryMeasurement = new DynamicTelemetryMeasurement(
                time,
                String.valueOf(groupId),
                String.valueOf(locationId),
                sensorEui,
                packet.sensorName(),
                fields
        );

        //InfluxDB 적재
        influxWriter.write(telemetryMeasurement);

        if (resolvedSensorId == null) {
            // 미등록 기기는 sensor_id가 없어 Rule Engine 매칭이 의미 없으므로 RabbitMQ 발행은 건너뜀
            return;
        }

        // RabbitMQ 논블로킹 디스패처로 telemetryMeasurement 전달
        TelemetryEventMessage telemetryEventMessage = new TelemetryEventMessage(
                groupId,
                locationId,
                resolvedSensorId,
                fields,
                time
        );

        telemetryPublisher.publish(telemetryEventMessage);
    }
}