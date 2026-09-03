package com.insighton.core.adapter.client.actuator.smartthings;

import com.insighton.core.adapter.client.actuator.smartthings.dto.SmartThingsCommandRequest;
import com.insighton.core.adapter.client.actuator.smartthings.dto.SmartThingsCommandResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

// SmartThings 호환 API로 실제 HTTP 요청을 보내는 얇은 클라이언트.
// base-url + Authorization 헤더는 ActuatorRestClientConfig의 smartThingsRestClient 빈이 이미 설정.
@Component
@RequiredArgsConstructor
public class SmartThingsApiClient {

    private final RestClient smartThingsRestClient;

    // POST /v1/devices/{id}/commands 전송. 4xx/5xx·연결오류·역직렬화오류는 SmartThingsApiException으로 감싼다
    public SmartThingsCommandResponse sendCommands(String deviceId, SmartThingsCommandRequest request) {
        try {
            return smartThingsRestClient.post()
                    .uri("/v1/devices/{deviceId}/commands", deviceId)
                    .body(request)
                    .retrieve()
                    .body(SmartThingsCommandResponse.class);
        } catch (RestClientResponseException e) {
            // 4xx/5xx 응답 - 상태코드와 본문을 담아 공통 예외로
            throw new SmartThingsApiException(
                    "SmartThings 명령 실패 (" + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);
        } catch (SmartThingsApiException e) {
            throw e;
        } catch (Exception e) {
            // 연결 실패, timeout, 역직렬화 오류 등
            throw new SmartThingsApiException("SmartThings 호출 중 오류: " + e.getMessage(), e);
        }
    }
}
