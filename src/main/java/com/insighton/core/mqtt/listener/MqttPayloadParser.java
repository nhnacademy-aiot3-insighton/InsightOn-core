package com.insighton.core.mqtt.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.mqtt.listener.dto.ChirpStackUplinkPacket;
import com.insighton.core.mqtt.listener.dto.CleanTelemetryPacket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
/**
 * MQTT 메서드 본문 파싱
 * 형식이 안 맞으면 빈 Optional 반환 후 로그 남기기
 */
public class MqttPayloadParser {
    private final ObjectMapper objectMapper;

    /**
     * MQTT 페이로드를 정제된 텔레메트리 패킷으로 변환한다.
     *
     * @param payload 바이트 배열 또는 UTF-8 문자열 형식의 MQTT 페이로드
     * @return 파싱에 성공하면 정제된 텔레메트리 패킷을 포함한 {@code Optional}, 실패하면 빈 {@code Optional}
     */
    public Optional<CleanTelemetryPacket> parse(Object payload) {
        try {
            byte[] bytes = switch (payload) {
                case byte[] b -> b;
                case String s -> s.getBytes(StandardCharsets.UTF_8);
                default -> throw new IllegalArgumentException("지원하지 않는 페이로드 타입 " + payload.getClass());
            };

            ChirpStackUplinkPacket raw = objectMapper.readValue(bytes, ChirpStackUplinkPacket.class);

            return Optional.of(new CleanTelemetryPacket(
                    raw.time(),
                    raw.deviceInfo().devEui(),
                    raw.deviceInfo().deviceName(),
                    raw.object()
            ));
        } catch (Exception e) {
            log.warn("MQTT 페이로드 파싱 실패", e);
            return Optional.empty();
        }
    }
}
