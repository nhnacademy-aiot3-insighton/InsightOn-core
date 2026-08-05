package com.insighton.core.mqtt.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.mqtt.listener.dto.ChirpStackUplinkPacket;
import com.insighton.core.mqtt.listener.dto.CleanTelemetryPacket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MQTT 페이로드(바이트/문자열)를 {@link ChirpStackUplinkPacket}으로 역직렬화한 뒤
 * {@link CleanTelemetryPacket}으로 정제하는 파서.
 * 형식이 안 맞으면 예외를 던지지 않고 빈 Optional을 반환하고 로그만 남김 — 패킷 하나의
 * 파싱 실패가 나머지 패킷 처리를 막지 않게 하기 위함.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MqttPayloadParser {
    private final ObjectMapper objectMapper;

    /**
     * 페이로드를 파싱해 정제된 텔레메트리 패킷으로 변환함.
     * ChirpStack 실제 와이어 포맷은 {@code devEui}/{@code deviceName}이 최상위가 아니라
     * {@code deviceInfo} 객체 안에 중첩돼 있어서, 우선 그 형태 그대로({@link ChirpStackUplinkPacket})
     * 역직렬화한 뒤, 이후 로직이 쓰기 편한 평평한 구조로 변환해서 반환함.
     *
     * @param payload MQTT 메시지 본문 (byte[] 또는 String만 지원)
     * @return 정제된 패킷, 지원하지 않는 타입이거나 JSON 형식이 안 맞으면 빈 Optional
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
