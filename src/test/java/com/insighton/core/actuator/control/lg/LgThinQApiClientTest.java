package com.insighton.core.actuator.control.lg;

import com.insighton.core.adapter.client.actuator.lg.LgThinQApiClient;
import com.insighton.core.adapter.client.actuator.lg.LgThinQApiException;
import com.insighton.core.adapter.client.actuator.lg.dto.LgThinQControlResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LgThinQApiClientTest {

    private static final String BASE_URL = "http://lg.test/lg";

    private MockRestServiceServer server;
    private LgThinQApiClient client;

    @BeforeEach
    void setUp() {
        // 실연동 config(ActuatorRestClientConfig)가 박는 공통 헤더를 흉내.
        // x-message-id / x-conditional-control 은 client가 요청마다 얹는다.
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bearer test-token")
                .defaultHeader("x-api-key", "test-api-key")
                .defaultHeader("x-client-id", "client-uuid")
                .defaultHeader("x-country", "KR");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new LgThinQApiClient(builder.build());
    }

    private static Map<String, Object> powerOn() {
        return Map.of("operation", Map.of("airConOperationMode", "POWER_ON"));
    }

    @Test
    @DisplayName("정상 200 - 공식 endpoint로 POST, 공통+요청별 헤더/payload 실림, 응답 파싱")
    void control_성공() {
        server.expect(requestTo(BASE_URL + "/devices/lg-aircon-001/control"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(header("x-api-key", "test-api-key"))
                .andExpect(header("x-conditional-control", "true"))
                .andExpect(header("x-message-id", Matchers.not(Matchers.emptyOrNullString())))
                .andExpect(jsonPath("$.operation.airConOperationMode").value("POWER_ON"))
                .andRespond(withSuccess(
                        "{\"messageId\":\"m-1\",\"timestamp\":\"2026-09-03T00:00:00Z\",\"response\":{}}",
                        MediaType.APPLICATION_JSON));

        LgThinQControlResponse response = client.control("lg-aircon-001", powerOn());

        assertThat(response.messageId()).isEqualTo("m-1");
        assertThat(response.error()).isNull();
        server.verify();
    }

    @Test
    @DisplayName("4xx 응답이면 LgThinQApiException (상태코드 포함)")
    void control_4xx() {
        server.expect(requestTo(BASE_URL + "/devices/missing/control"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .body("{\"error\":{\"code\":\"2000\",\"message\":\"device not found\"}}"));

        assertThatThrownBy(() -> client.control("missing", powerOn()))
                .isInstanceOf(LgThinQApiException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("5xx 응답이면 LgThinQApiException")
    void control_5xx() {
        server.expect(requestTo(BASE_URL + "/devices/lg-aircon-001/control"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.control("lg-aircon-001", powerOn()))
                .isInstanceOf(LgThinQApiException.class);
    }
}
