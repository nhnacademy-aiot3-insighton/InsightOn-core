package com.insighton.core.adapter.client.actuator.lg;

import com.insighton.core.adapter.client.actuator.lg.dto.LgThinQControlRequest;
import com.insighton.core.adapter.client.actuator.lg.dto.LgThinQControlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

// LG ThinQ 호환 API로 실제 HTTP 요청을 보내는 얇은 클라이언트.
// base-url + Authorization 헤더는 ActuatorRestClientConfig의 lgThinQRestClient 빈이 설정.
@Component
@RequiredArgsConstructor
public class LgThinQApiClient {

    private final RestClient lgThinQRestClient;

    public LgThinQControlResponse control(String deviceId, LgThinQControlRequest request) {
        try {
            return lgThinQRestClient.post()
                    .uri("/devices/{deviceId}/control", deviceId)
                    .body(request)
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
}
