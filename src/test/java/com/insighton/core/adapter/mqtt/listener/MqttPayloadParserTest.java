package com.insighton.core.adapter.mqtt.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.adapter.mqtt.listener.dto.CleanTelemetryPacket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * ChirpStack 실제 와이어 포맷(devEui/deviceName이 deviceInfo 안에 중첩) 파싱 검증.
 * 이 파서의 계약은 "형식이 안 맞아도 절대 예외를 던지지 않고 빈 Optional을 반환한다"이므로
 * 그 계약이 실제로 지켜지는지가 핵심.
 */
class MqttPayloadParserTest {

    private final MqttPayloadParser parser = new MqttPayloadParser(new ObjectMapper());

    @Test
    void ChirpStack_표준_페이로드를_정제된_패킷으로_변환한다() {
        String payload = """
                {
                  "time": "2026-09-04T00:00:00Z",
                  "deviceInfo": { "devEui": "eui-1", "deviceName": "sensor-1" },
                  "object": { "co2": 800, "humidity": 55.5 }
                }
                """;

        Optional<CleanTelemetryPacket> result = parser.parse(payload);

        assertThat(result).isPresent();
        CleanTelemetryPacket packet = result.get();
        assertThat(packet.time()).isEqualTo("2026-09-04T00:00:00Z");
        assertThat(packet.sensorEui()).isEqualTo("eui-1");
        assertThat(packet.sensorName()).isEqualTo("sensor-1");
        assertThat(packet.object()).containsEntry("co2", 800).containsEntry("humidity", 55.5);
    }

    @Test
    void byte배열_페이로드도_동일하게_파싱된다() {
        String json = """
                {"time":"2026-09-04T00:00:00Z","deviceInfo":{"devEui":"eui-1","deviceName":"sensor-1"},"object":{"co2":800}}
                """;

        Optional<CleanTelemetryPacket> result = parser.parse(json.getBytes(StandardCharsets.UTF_8));

        assertThat(result).isPresent();
        assertThat(result.get().sensorEui()).isEqualTo("eui-1");
    }

    @Test
    void object_필드가_없어도_파싱은_성공하고_object는_null이다() {
        // codec 디코드 실패나 keep-alive성 업링크에서 실제로 발생하는 케이스
        String payload = """
                {
                  "time": "2026-09-04T00:00:00Z",
                  "deviceInfo": { "devEui": "eui-1", "deviceName": "sensor-1" }
                }
                """;

        Optional<CleanTelemetryPacket> result = parser.parse(payload);

        assertThat(result).isPresent();
        assertThat(result.get().object()).isNull();
    }

    @Test
    void deviceInfo가_없으면_예외_대신_빈_Optional을_반환한다() {
        String payload = """
                {"time": "2026-09-04T00:00:00Z", "object": {"co2": 800}}
                """;

        Optional<CleanTelemetryPacket> result = parser.parse(payload);

        assertThat(result).isEmpty();
    }

    @Test
    void JSON_형식이_깨져도_예외_대신_빈_Optional을_반환한다() {
        Optional<CleanTelemetryPacket> result = parser.parse("{ 이건 JSON이 아님");

        assertThat(result).isEmpty();
    }

    @Test
    void 지원하지_않는_페이로드_타입이면_빈_Optional을_반환한다() {
        Optional<CleanTelemetryPacket> result = parser.parse(12345);

        assertThat(result).isEmpty();
    }
}
