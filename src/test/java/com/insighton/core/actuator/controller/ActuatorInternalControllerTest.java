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
import com.insighton.core.domain.location.entity.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActuatorInternalController.class)
class ActuatorInternalControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ActuatorRunLogService actuatorRunLogService;
    @MockBean private ActuatorService actuatorService;
    @MockBean private ActuatorRepository actuatorRepository;

    @Test
    @DisplayName("run-logs 조회 성공")
    void 실행로그_조회_성공() throws Exception {
        ActuatorRunLogInternalResponse log = new ActuatorRunLogInternalResponse(
                42L, 7L, "AIRCON", "SET_TEMPERATURE", "24.0", "USER", OffsetDateTime.now());
        given(actuatorRunLogService.getRunLogsForReport(anyList(), any(), any()))
                .willReturn(List.of(log));

        mockMvc.perform(get("/internal/actuators/run-logs")
                        .param("locationIds", "42")
                        .param("from", "2026-07-30T00:00:00+09:00")
                        .param("to", "2026-08-06T00:00:00+09:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].locationId").value(42L));
    }

    @Test
    @DisplayName("location+actuatorType 상태변경 성공 - 대상 전체에 적용")
    void 위치기반_상태변경_성공() throws Exception {
        Location location = mock(Location.class);
        Actuator actuatorA = Actuator.builder().actuatorId(7L).actuatorType(ActuatorType.AIRCON).location(location).build();
        Actuator actuatorB = Actuator.builder().actuatorId(9L).actuatorType(ActuatorType.AIRCON).location(location).build();

        given(actuatorRepository.findByLocationLocationIdAndActuatorType(42L, ActuatorType.AIRCON))
                .willReturn(List.of(actuatorA, actuatorB));

        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "OFF", ExecutedByType.AI_SYSTEM);

        mockMvc.perform(put("/internal/locations/{location-id}/actuators/state", 42L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(actuatorService).updateActuatorState(eq(null), eq(null), eq(7L), anyMap(), eq(ExecutedByType.AI_SYSTEM));
        verify(actuatorService).updateActuatorState(eq(null), eq(null), eq(9L), anyMap(), eq(ExecutedByType.AI_SYSTEM));
    }

    @Test
    @DisplayName("callerService가 USER면 403")
    void 위치기반_상태변경_USER차단() throws Exception {
        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "OFF", ExecutedByType.USER);

        mockMvc.perform(put("/internal/locations/{location-id}/actuators/state", 42L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(actuatorService, never())
                .updateActuatorState(any(), any(), any(), anyMap(), any());
    }

    @Test
    @DisplayName("해당 위치에 그 타입 액추에이터가 없으면 404")
    void 대상없음_404() throws Exception {
        given(actuatorRepository.findByLocationLocationIdAndActuatorType(42L, ActuatorType.VENTILATION_FAN))
                .willReturn(List.of());

        ActuatorCommandRequest request = new ActuatorCommandRequest("VENTILATION_FAN", "power", "OFF", ExecutedByType.AI_SYSTEM);

        mockMvc.perform(put("/internal/locations/{location-id}/actuators/state", 42L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("존재하지 않는 actuatorType이면 400")
    void 잘못된_actuatorType_400() throws Exception {
        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCONN", "power", "OFF", ExecutedByType.AI_SYSTEM);

        mockMvc.perform(put("/internal/locations/{location-id}/actuators/state", 42L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("지원하지 않는 명령값이면 400")
    void 잘못된값_400() throws Exception {
        Location location = mock(Location.class);
        Actuator actuator = Actuator.builder().actuatorId(7L).actuatorType(ActuatorType.AIR_PURIFIER).location(location).build();

        given(actuatorRepository.findByLocationLocationIdAndActuatorType(42L, ActuatorType.AIR_PURIFIER))
                .willReturn(List.of(actuator));

        ActuatorCommandRequest request = new ActuatorCommandRequest("AIR_PURIFIER", "temperature", "24.0", ExecutedByType.AI_SYSTEM);

        mockMvc.perform(put("/internal/locations/{location-id}/actuators/state", 42L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}