package com.insighton.core.actuator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.controller.internal.ActuatorInternalController;
import com.insighton.core.domain.actuatorrunlogs.dto.ActuatorRunLogInternalResponse;
import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuatorrunlogs.service.ActuatorRunLogService;
import com.insighton.core.domain.actuators.dto.ActuatorCommandRequest;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.exception.ActuatorLocationsActuatorTypeNotFound;
import com.insighton.core.domain.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.domain.actuators.exception.InvalidServiceCredentialException;
import com.insighton.core.domain.location.exception.LocationNotFoundException;
import com.insighton.core.usecase.actuator.UpdateActuatorStateByGroupUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
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
    @MockitoBean private UpdateActuatorStateByGroupUseCase updateActuatorStateByGroupUseCase;

    @Test
    @DisplayName("AI 리포트용 실행 로그 조회 성공 - 인증 헤더 없이도 호출 가능")
    void 실행로그조회_성공() throws Exception {
        ActuatorRunLogInternalResponse log = new ActuatorRunLogInternalResponse(
                20L, 1L, "AIRCON", "POWER_STATUS", "ON", "USER", OffsetDateTime.now());
        given(actuatorRunLogService.getRunLogsForReport(eq(List.of(20L)), any(), any()))
                .willReturn(List.of(log));

        mockMvc.perform(get("/internal/v1/actuators/run-logs")
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
        mockMvc.perform(get("/internal/v1/actuators/run-logs")
                        .param("locationIds", "20"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("시스템 상태변경 - groupId/locationId를 그대로 유스케이스에 위임하고 200 반환")
    void 시스템상태변경_성공_위임() throws Exception {
        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "ON", ExecutedByType.RULE_ENGINE);

        mockMvc.perform(put("/internal/v1/groups/{groupId}/locations/{locationId}/actuators/state", 5L, 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(updateActuatorStateByGroupUseCase).execute(5L, 20L, request);
    }

    @Test
    @DisplayName("시스템 상태변경 - USER가 호출하면 403 (유스케이스 예외가 그대로 매핑됨)")
    void 시스템상태변경_USER호출_403() throws Exception {
        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "ON", ExecutedByType.USER);
        willThrow(new InvalidServiceCredentialException("이 내부 API는 USER가 호출할 수 없습니다"))
                .given(updateActuatorStateByGroupUseCase).execute(5L, 20L, request);

        mockMvc.perform(put("/internal/v1/groups/{groupId}/locations/{locationId}/actuators/state", 5L, 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("시스템 상태변경 - groupId+locationId 소유권 불일치면 404")
    void 시스템상태변경_그룹불일치_404() throws Exception {
        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "ON", ExecutedByType.RULE_ENGINE);
        willThrow(LocationNotFoundException.notFoundLocationByLocationId(20L))
                .given(updateActuatorStateByGroupUseCase).execute(5L, 20L, request);

        mockMvc.perform(put("/internal/v1/groups/{groupId}/locations/{locationId}/actuators/state", 5L, 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("시스템 상태변경 - 존재하지 않는 actuatorType이면 400")
    void 시스템상태변경_알수없는타입_400() throws Exception {
        ActuatorCommandRequest request = new ActuatorCommandRequest("UNKNOWN_TYPE", "power", "ON", ExecutedByType.RULE_ENGINE);
        willThrow(new InvalidActuatorValueException("알 수 없는 actuatorType입니다: UNKNOWN_TYPE"))
                .given(updateActuatorStateByGroupUseCase).execute(5L, 20L, request);

        mockMvc.perform(put("/internal/v1/groups/{groupId}/locations/{locationId}/actuators/state", 5L, 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("시스템 상태변경 - 해당 위치+타입 조합의 액추에이터가 없으면 404")
    void 시스템상태변경_대상없음_404() throws Exception {
        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "ON", ExecutedByType.RULE_ENGINE);
        willThrow(new ActuatorLocationsActuatorTypeNotFound(20L, ActuatorType.AIRCON))
                .given(updateActuatorStateByGroupUseCase).execute(5L, 20L, request);

        mockMvc.perform(put("/internal/v1/groups/{groupId}/locations/{locationId}/actuators/state", 5L, 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("시스템 상태변경 - 허용 안 된 명령값이면 400")
    void 시스템상태변경_비허용값_400() throws Exception {
        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "EXPLODE", ExecutedByType.RULE_ENGINE);
        willThrow(new InvalidActuatorValueException("허용되지 않은 값"))
                .given(updateActuatorStateByGroupUseCase).execute(5L, 20L, request);

        mockMvc.perform(put("/internal/v1/groups/{groupId}/locations/{locationId}/actuators/state", 5L, 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
