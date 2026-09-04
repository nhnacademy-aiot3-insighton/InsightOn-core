package com.insighton.core.adapter.mqtt.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.insighton.core.adapter.influx.TelemetryInfluxWriter;
import com.insighton.core.adapter.mqtt.cache.GatewayGroupMappingCache;
import com.insighton.core.adapter.mqtt.cache.SensorLookupCacheService;
import com.insighton.core.adapter.mqtt.cache.dto.SensorCacheEntry;
import com.insighton.core.adapter.mqtt.connection.DynamicMqttGatewayManager;
import com.insighton.core.adapter.mqtt.connection.GatewayHeartbeatTracker;
import com.insighton.core.adapter.mqtt.connection.SensorHeartbeatTracker;
import com.insighton.core.adapter.mqtt.listener.dto.CleanTelemetryPacket;
import com.insighton.core.adapter.mqtt.listener.dto.TelemetryEventMessage;
import com.insighton.core.domain.sensors.service.SensorService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

@ExtendWith(MockitoExtension.class)
class GatewayPacketInboundHandlerTest {

    @Mock
    private MqttPayloadParser payloadParser;
    @Mock
    private SensorLookupCacheService sensorLookupCacheService;
    @Mock
    private GatewayGroupMappingCache groupMappingCache;
    @Mock
    private GatewayHeartbeatTracker gatewayHeartbeatTracker;
    @Mock
    private TelemetryPublisher telemetryPublisher;
    @Mock
    private TelemetryInfluxWriter influxWriter;
    @Mock
    private SensorService sensorService;
    @Mock
    private SensorHeartbeatTracker sensorHeartbeatTracker;

    private GatewayPacketInboundHandler handler() {
        return new GatewayPacketInboundHandler(payloadParser, sensorLookupCacheService, groupMappingCache,
                gatewayHeartbeatTracker, telemetryPublisher, influxWriter, sensorService, sensorHeartbeatTracker);
    }

    private Message<byte[]> messageWithGatewayId() {
        return MessageBuilder.withPayload(new byte[0])
                .setHeader(DynamicMqttGatewayManager.GATEWAY_ID_HEADER, 1L)
                .build();
    }

    @Test
    void object가_null인_패킷은_적재_발행_없이_드롭된다() {
        // given - ChirpStack object가 null인 경우 (codec 디코드 실패/keep-alive성 업링크)
        given(payloadParser.parse(any()))
                .willReturn(Optional.of(new CleanTelemetryPacket("2026-09-04T00:00:00Z", "eui-1", "sensor-1", null)));
        given(groupMappingCache.get(1L)).willReturn(Optional.of(10L));
        given(sensorLookupCacheService.lookup("eui-1"))
                .willReturn(Optional.of(new SensorCacheEntry(100L, "eui-1", 1L, 200L)));

        // when
        handler().handleMessage(messageWithGatewayId());

        // then - 하트비트/센서조회는 정상 진행되지만 측정값이 없으니 적재/발행은 안 됨
        verify(sensorHeartbeatTracker).recordHeartbeat(100L);
        verify(influxWriter, never()).write(any());
        verify(telemetryPublisher, never()).publish(any());
    }

    @Test
    void 측정값이_있는_패킷은_정상적으로_적재_발행된다() {
        // given
        Map<String, Object> fields = Map.of("co2", 800);
        given(payloadParser.parse(any()))
                .willReturn(Optional.of(new CleanTelemetryPacket("2026-09-04T00:00:00Z", "eui-1", "sensor-1", fields)));
        given(groupMappingCache.get(1L)).willReturn(Optional.of(10L));
        given(sensorLookupCacheService.lookup("eui-1"))
                .willReturn(Optional.of(new SensorCacheEntry(100L, "eui-1", 1L, 200L)));

        // when
        handler().handleMessage(messageWithGatewayId());

        // then
        verify(influxWriter).write(any());
        verify(telemetryPublisher).publish(any(TelemetryEventMessage.class));
    }
}
