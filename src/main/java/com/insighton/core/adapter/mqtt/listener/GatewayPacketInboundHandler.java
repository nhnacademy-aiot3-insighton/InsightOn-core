package com.insighton.core.adapter.mqtt.listener;

import com.insighton.core.adapter.influx.TelemetryInfluxWriter;
import com.insighton.core.adapter.influx.dto.DynamicTelemetryMeasurement;
import com.insighton.core.adapter.mqtt.cache.SensorLookupCacheService;
import com.insighton.core.adapter.mqtt.cache.GatewayGroupMappingCache;
import com.insighton.core.adapter.mqtt.cache.dto.SensorCacheEntry;
import com.insighton.core.adapter.mqtt.connection.DynamicMqttGatewayManager;
import com.insighton.core.adapter.mqtt.connection.GatewayHeartbeatTracker;
import com.insighton.core.adapter.mqtt.connection.SensorHeartbeatTracker;
import com.insighton.core.adapter.mqtt.listener.dto.CleanTelemetryPacket;
import com.insighton.core.adapter.mqtt.listener.dto.TelemetryEventMessage;
import com.insighton.core.domain.sensors.service.SensorService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
 * 정제된 텔레메트리 이벤트를 Rule Engine 전파용 RabbitMQ x-consistent-hash Exchange에 비동기로 발행함.
 * 같은 locationId가 같은 큐로 가도록 라우팅 키는 비워두고, locationId를 헤더로 실어서
 * 그 값 기준으로 해시 라우팅되게 함(exchange 선언 시 hash-header 인자로 지정).
 * {@code @Async}로 비동기 처리하며, 발행 실패는 예외를 던지지 않고 로그만 남기고 드롭함
 * (Fail-Silent) — RabbitMQ 장애가 MQTT 리스너 스레드를 막지 않게 하기 위함.
 *
 */
@Component("gatewayPacketHandler")
@RequiredArgsConstructor
@Slf4j
public class GatewayPacketInboundHandler implements MessageHandler {

    private final MqttPayloadParser payloadParser;
    private final SensorLookupCacheService sensorLookupCacheService;
    private final GatewayGroupMappingCache groupMappingCache;
    private final GatewayHeartbeatTracker gatewayHeartbeatTracker;
    private final TelemetryPublisher telemetryPublisher;
    private final TelemetryInfluxWriter influxWriter;
    private final SensorService sensorService;
    private final SensorHeartbeatTracker sensorHeartbeatTracker;

    /**
     * MQTT로부터 수신한 메시지 하나를 처리함. 실제 로직은 {@link #handlePacket(Message)}에 있고
     * 이 메서드는 예외 격리만 담당함 — 패킷 처리 중 발생한 예외가 Paho 콜백까지 올라가면
     * MQTT 연결 자체가 끊기고(Lost connection), Reconciler가 재등록해도 다음 패킷에서 또 터지는
     * 플래핑이 되므로 여기서 반드시 차단해야 함.
     *
     * @param message 수신한 MQTT 메시지
     */
    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        try {
            handlePacket(message);
        } catch (Exception e) {
            // 패킷 하나의 처리 실패가 Paho 콜백까지 전파되면 MQTT 연결 자체가 끊기고
            // (Lost connection → Reconciler 재등록 → 다시 실패) 플래핑이 되므로 여기서 차단하고 드롭함
            log.error("패킷 처리 실패, 드롭 (topic = {})",
                    message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC), e);
        }
    }

    /**
     * 패킷 하나에 대한 실제 처리. 순서대로:
     * <ol>
     *     <li>{@code gatewayId} 헤더 확인 — 없으면 groupId를 구할 수 없어 즉시 드롭.
     *         있으면 파싱 성공 여부와 무관하게 먼저 하트비트를 기록함</li>
     *     <li>{@link MqttPayloadParser}로 페이로드를 {@link CleanTelemetryPacket}으로 파싱,
     *         실패하면 드롭</li>
     *     <li>{@link GatewayGroupMappingCache}로 groupId 확정 — Auto-Provisioning 인자로
     *         필요해서 센서 조회보다 먼저 구함</li>
     *     <li>{@link SensorLookupCacheService}로 센서 조회, 캐시에 없으면
     *         {@link SensorService#autoProvision} 으로 즉시 등록하고 캐시까지 채워서 받음</li>
     *     <li>location 미배치면 드롭 — 신규 등록 센서는 항상 여기 해당함. 위치를 모르는 측정값은
     *         대시보드/룰엔진 어디에도 쓸 수 없고, InfluxDB는 location_id가 태그라 나중에
     *         소급 수정도 불가능하므로 적재 전에 버림</li>
     *     <li>InfluxDB 적재 + RabbitMQ 발행 (둘 다 비동기, Fail-Silent)</li>
     * </ol>
     *
     * @param message 수신한 MQTT 메시지
     */
    private void handlePacket(Message<?> message) {
        Long gatewayId = message.getHeaders().get(DynamicMqttGatewayManager.GATEWAY_ID_HEADER, Long.class);

        if(gatewayId == null) {
            log.warn("gatewayId 헤더 없음 — DynamicMqttGatewayManager의 enrichHeaders 누락 의심 (topic = {})",
                    message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC));
            return;   // gatewayId 없이는 groupId도 못 구해 Auto-Provisioning 불가
        }

        gatewayHeartbeatTracker.recordHeartbeat(gatewayId);

        Optional<CleanTelemetryPacket> optionalPacket = payloadParser.parse(message.getPayload());

        if(optionalPacket.isEmpty()) {
            log.warn("MQTT 페이로드 파싱 실패, 패킷 드롭 (topic = {})",
                    message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC));
            return;
        }

        CleanTelemetryPacket packet = optionalPacket.get();
        String sensorEui = packet.sensorEui();

        Optional<Long> optionalGroupId = groupMappingCache.get(gatewayId);

        if(optionalGroupId.isEmpty()) {
            log.warn("게이트웨이 {}의 group 매핑이 캐시에 없어 패킷 드롭 (sensorEui = {})", gatewayId, sensorEui);
            return;
        }

        Long groupId = optionalGroupId.get();

        Map<String, Object> fields = packet.object() != null ? packet.object() : Map.of();

        SensorCacheEntry sensor = sensorLookupCacheService.lookup(sensorEui)
                .orElseGet(() -> sensorService.autoProvision(
                        gatewayId,
                        groupId,
                        sensorEui,
                        packet.sensorName(),
                        fields.keySet()
                ));

        sensorHeartbeatTracker.recordHeartbeat(sensor.sensorId());

        Long locationId = sensor.locationId();

        if(locationId == null) {
            // 신규 등록 센서는 항상 여기서 걸림 — 유저가 대시보드에서 공간을 배치해야 이후 패킷이 흐름.
            // 위치를 모르는 측정값은 대시보드/룰엔진/리포트 어디에도 못 쓰고, InfluxDB는 location_id가
            // 태그라 나중에 소급 수정도 불가능하므로 적재 전에 드롭함.
            log.info("공간 미배치 센서 패킷 드롭 (sensorEui = {}, sensorId = {})", sensorEui, sensor.sensorId());
            return;
        }

        if(fields.isEmpty()) {
            // ChirpStack object가 null인 경우(codec 디코드 실패, keep-alive성 업링크 등) 여기 해당함.
            // 하트비트/auto-provision은 위에서 이미 처리했으니 생존 신호와 센서 등록은 반영되고,
            // 측정값 자체가 없는 적재/발행만 막음 — InfluxDB에 빈 Point가 만들어져 조용히 스킵되거나
            // Rule Engine이 빈 fields를 역직렬화하다 실패하는 걸 소스에서 미리 차단함
            log.info("측정값 없는 패킷 드롭 (fields 비어있음, sensorEui = {}, sensorId = {})", sensorEui, sensor.sensorId());
            return;
        }

        Instant time = Instant.parse(packet.time());

        influxWriter.write(new DynamicTelemetryMeasurement(
                time,
                String.valueOf(groupId),
                String.valueOf(locationId),
                sensorEui,
                packet.sensorName(),
                fields
        ));

        telemetryPublisher.publish(new TelemetryEventMessage(
                groupId, locationId, sensor.sensorId(), fields, time));
    }
}