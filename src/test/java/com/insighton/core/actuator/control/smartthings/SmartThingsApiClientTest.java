package com.insighton.core.actuator.control.smartthings;

import com.insighton.core.adapter.client.actuator.smartthings.SmartThingsApiClient;
import com.insighton.core.adapter.client.actuator.smartthings.SmartThingsApiException;
import com.insighton.core.adapter.client.actuator.smartthings.dto.SmartThingsCommandRequest;
import com.insighton.core.adapter.client.actuator.smartthings.dto.SmartThingsCommandResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SmartThingsApiClientTest {

    private static final String BASE_URL = "http://smartthings.test/smartthings";

    private MockRestServiceServer server;
    private SmartThingsApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bearer test-token");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new SmartThingsApiClient(builder.build());
    }

    private static SmartThingsCommandRequest onRequest() {
        return new SmartThingsCommandRequest(List.of(
                new SmartThingsCommandRequest.Command("main", "switch", "on", List.of())));
    }

    @Test
    @DisplayName("정상 200 - 공식 endpoint/헤더/payload로 POST하고 results를 파싱한다")
    void sendCommands_성공() {
        server.expect(requestTo(BASE_URL + "/v1/devices/st-aircon-001/commands"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(jsonPath("$.commands[0].component").value("main"))
                .andExpect(jsonPath("$.commands[0].capability").value("switch"))
                .andExpect(jsonPath("$.commands[0].command").value("on"))
                .andRespond(withSuccess("{\"results\":[{\"id\":\"abc\",\"status\":\"ACCEPTED\"}]}",
                        MediaType.APPLICATION_JSON));

        SmartThingsCommandResponse response = client.sendCommands("st-aircon-001", onRequest());

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).status()).isEqualTo("ACCEPTED");
        server.verify();
    }

    @Test
    @DisplayName("4xx 응답이면 SmartThingsApiException (상태코드/본문 포함)")
    void sendCommands_4xx() {
        server.expect(requestTo(BASE_URL + "/v1/devices/missing/commands"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("{\"error\":\"device not found\"}"));

        assertThatThrownBy(() -> client.sendCommands("missing", onRequest()))
                .isInstanceOf(SmartThingsApiException.class)
                .hasMessageContaining("404");
        server.verify();
    }

    @Test
    @DisplayName("5xx 응답이면 SmartThingsApiException")
    void sendCommands_5xx() {
        server.expect(requestTo(BASE_URL + "/v1/devices/st-aircon-001/commands"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.sendCommands("st-aircon-001", onRequest()))
                .isInstanceOf(SmartThingsApiException.class);
        server.verify();
    }
}
