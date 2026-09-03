package com.insighton.core.adapter.client.actuator.lg;

import com.insighton.core.adapter.client.actuator.lg.dto.LgThinQControlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

// LG ThinQ Connect API로 HTTP 요청을 보내는 얇은 클라이언트.
// base-url + 공통 헤더(Authorization, x-api-key, x-client-id, x-country, x-service-phase)는 lgThinQRestClient 빈이 설정.
// 요청마다 얹는 것: x-message-id(매번 새 base64url uuid), x-conditional-control:true(제어).
// 실제: POST https://api-kic.lgthinq.com/devices/{id}/control  (docs/provider-contract.md §1·§2)
@Component
@RequiredArgsConstructor
public class LgThinQApiClient {

    private final RestClient lgThinQRestClient;

    // POST /devices/{id}/control 전송 (x-message-id·x-conditional-control 추가). 오류는 LgThinQApiException으로 감싼다
    public LgThinQControlResponse control(String deviceId, Map<String, Object> payload) {
        try {
            return lgThinQRestClient.post()
                    .uri("/devices/{deviceId}/control", deviceId)
                    .header("x-message-id", newMessageId())
                    .header("x-conditional-control", "true")
                    .body(payload)
                    .retrieve()
                    .body(LgThinQControlResponse.class);
        } catch (RestClientResponseException e) {
            throw new LgThinQApiException(
                    "LG ThinQ 제어 실패 (" + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);
        } catch (LgThinQApiException e) {
            throw e;
        } catch (Exception e) {
            throw new LgThinQApiException("LG ThinQ 호출 중 오류: " + e.getMessage(), e);
        }
    }

    // 실제 SDK와 동일: base64url(uuid.bytes) 에서 끝의 '==' 패딩 2자를 제거
    private static String newMessageId() {
        UUID uuid = UUID.randomUUID();
        byte[] bytes = new byte[16];
        long hi = uuid.getMostSignificantBits();
        long lo = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (hi >>> (8 * (7 - i)));
            bytes[8 + i] = (byte) (lo >>> (8 * (7 - i)));
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
