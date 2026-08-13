package com.insighton.core.actuator.controller;

import com.insighton.core.controller.internal.ActuatorInternalController;
import com.insighton.core.domain.actuatorrunlogs.dto.ActuatorRunLogInternalResponse;
import com.insighton.core.domain.actuatorrunlogs.service.ActuatorRunLogService;
import com.insighton.core.domain.actuators.repository.ActuatorRepository;
import com.insighton.core.domain.actuators.service.ActuatorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// AI 리포트 배치 전용 조회 엔드포인트만 검증 - updateActuatorStateBySystem은 별도 기존 로직이라 이 변경 범위 밖
@WebMvcTest(ActuatorInternalController.class)
class ActuatorInternalControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ActuatorRunLogService actuatorRunLogService;
    @MockBean private ActuatorService actuatorService;
    @MockBean private ActuatorRepository actuatorRepository;

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
}
