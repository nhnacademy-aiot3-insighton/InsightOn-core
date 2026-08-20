package com.insighton.core.actuator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.controller.internal.ActuatorInternalController;
import com.insighton.core.domain.actuatorrunlogs.dto.ActuatorRunLogInternalResponse;
import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuatorrunlogs.service.ActuatorRunLogService;
import com.insighton.core.domain.actuators.dto.ActuatorCommandRequest;
import com.insighton.core.domain.actuators.entity.Actuator;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.repository.ActuatorRepository;
import com.insighton.core.domain.actuators.service.ActuatorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActuatorInternalController.class)
class ActuatorInternalControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ActuatorRunLogService actuatorRunLogService;
    @MockitoBean private ActuatorService actuatorService;
    @MockitoBean private ActuatorRepository actuatorRepository;

    @Test
    @DisplayName("AI 리포트용 실행 로그 조회 성공 - 인증 헤더 없이도 호출 가능")
    void 실행로그조회_성공() throws Exception {
        ActuatorRunLogInternalResponse log = new ActuatorRunLogInternalResponse(
                20L, 1L, "AIRCON", "POWER_STATUS", "ON", "USER", OffsetDateTime.now());
        given(actuatorRunLogService.getRunLogsForReport(eq(List.of(20L)), any(), any()))
                .willReturn(List.of(log));

        mockMvc.perform(get("/internal/actuators/run-logs")
                        .param("locationIds", "20")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-01-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].actuatorId").value(1L));
    }

    @Test
    @DisplayName("필수 쿼리 파라미터가 없으면 400")
    void 실행로그조회_파라미터누락_400() throws Exception {
        mockMvc.perform(get("/internal/actuators/run-logs")
                        .param("locationIds", "20"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("시스템 상태변경 - USER가 호출하면 403")
    void 시스템상태변경_USER호출_403() throws Exception {
        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "ON", ExecutedByType.USER);

        mockMvc.perform(put("/internal/locations/{location-id}/actuators/state", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("시스템 상태변경 - 존재하지 않는 actuatorType이면 400")
    void 시스템상태변경_알수없는타입_400() throws Exception {
        ActuatorCommandRequest request = new ActuatorCommandRequest("UNKNOWN_TYPE", "power", "ON", ExecutedByType.RULE_ENGINE);

        mockMvc.perform(put("/internal/locations/{location-id}/actuators/state", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("시스템 상태변경 - 해당 위치+타입 조합의 액추에이터가 없으면 404")
    void 시스템상태변경_대상없음_404() throws Exception {
        given(actuatorRepository.findByLocationLocationIdAndActuatorType(20L, ActuatorType.AIRCON))
                .willReturn(List.of());

        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "ON", ExecutedByType.RULE_ENGINE);

        mockMvc.perform(put("/internal/locations/{location-id}/actuators/state", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("시스템 상태변경 - 허용 안 된 명령값이면 400, 상태변경 호출 안 함")
    void 시스템상태변경_비허용값_400() throws Exception {
        Actuator actuator = Actuator.builder().actuatorId(1L).actuatorType(ActuatorType.AIRCON).build();
        given(actuatorRepository.findByLocationLocationIdAndActuatorType(20L, ActuatorType.AIRCON))
                .willReturn(List.of(actuator));

        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "EXPLODE", ExecutedByType.RULE_ENGINE);

        mockMvc.perform(put("/internal/locations/{location-id}/actuators/state", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(actuatorService, never()).updateActuatorState(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("시스템 상태변경 - 정상 처리, 같은 위치+타입 액추에이터 전부에 상태 반영")
    void 시스템상태변경_성공() throws Exception {
        Actuator a1 = Actuator.builder().actuatorId(1L).actuatorType(ActuatorType.AIRCON).build();
        Actuator a2 = Actuator.builder().actuatorId(2L).actuatorType(ActuatorType.AIRCON).build();
        given(actuatorRepository.findByLocationLocationIdAndActuatorType(20L, ActuatorType.AIRCON))
                .willReturn(List.of(a1, a2));

        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "ON", ExecutedByType.RULE_ENGINE);

        mockMvc.perform(put("/internal/locations/{location-id}/actuators/state", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(actuatorService).updateActuatorState(null, 1L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null);
        verify(actuatorService).updateActuatorState(null, 2L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null);
    }
}
