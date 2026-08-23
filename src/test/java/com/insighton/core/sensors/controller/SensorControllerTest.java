package com.insighton.core.sensors.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.controller.api.SensorController;
import com.insighton.core.domain.location.exception.LocationNotFoundException;
import com.insighton.core.domain.sensors.dto.SensorResponse;
import com.insighton.core.domain.sensors.dto.SensorUpdateRequest;
import com.insighton.core.domain.sensors.exception.InvalidSensorValueException;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import com.insighton.core.usecase.sensor.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SensorController.class)
class SensorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GetSensorUseCase getSensorUseCase;
    @MockitoBean
    private SearchSensorUseCase searchSensorUseCase;
    @MockitoBean
    private GetUnassignedSensorsUseCase getUnassignedSensorsUseCase;
    @MockitoBean
    private UpdateSensorUseCase updateSensorUseCase;
    @MockitoBean
    private DeleteSensorUseCase deleteSensorUseCase;
    @MockitoBean
    private DeleteAllSensorUseCase deleteAllSensorUseCase;

    private SensorResponse sampleResponse() {
        return new SensorResponse(
                1L, 10L, 20L,
                "EUI-0001", "4층 CO2 센서",
                OffsetDateTime.now()
        );
    }

    @Test
    @DisplayName("단일 센서 조회 성공")
    void 센서_조회_성공() throws Exception {
        given(getSensorUseCase.getSensorById(1L, 1L)).willReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/sensor/{id}", 1L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensorId").value(1L))
                .andExpect(jsonPath("$.sensorEui").value("EUI-0001"));
    }

    @Test
    @DisplayName("존재하지 않는 센서 조회 시 404")
    void 센서_조회_실패_404() throws Exception {
        given(getSensorUseCase.getSensorById(anyLong(), anyLong()))
                .willThrow(new SensorNotFoundException(999L));

        mockMvc.perform(get("/api/v1/sensor/{id}", 999L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("통합 검색은 groupId가 필수 쿼리 파라미터")
    void 검색_groupId_누락시_400() throws Exception {
        mockMvc.perform(get("/api/v1/sensor/search")
                        .header("X-USER-ID", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("검색 성공 - locationName/sensorName이 SensorUpdateRequest로 묶여서 전달됨")
    void 검색_성공() throws Exception {
        given(searchSensorUseCase.searchSensors(eq(1L), eq(5L), isNull(), isNull(), any(SensorUpdateRequest.class)))
                .willReturn(java.util.List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/sensor/search")
                        .header("X-USER-ID", 1L)
                        .param("groupId", "5")
                        .param("locationName", "4층")
                        .param("sensorName", "CO2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(searchSensorUseCase).searchSensors(1L, 5L, null, null, new SensorUpdateRequest("4층", "CO2"));
    }

//    @Test
//    @DisplayName("장소 미배정 센서 목록 조회 성공")
//    void 장소미배정_조회_성공() throws Exception {
//        given(getUnassignedSensorsUseCase.getUnassignedSensors(1L, 5L)).willReturn(List.of(sampleResponse()));
//
//        mockMvc.perform(get("/api/v1/sensor/unassigned")
//                        .header("X-USER-ID", 1L)
//                        .param("groupId", "5"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(1))
//                .andExpect(jsonPath("$[0].sensorId").value(1L));
//    }

    @Test
    @DisplayName("위치만 수정 성공")
    void 위치만_수정_성공() throws Exception {
        SensorUpdateRequest request = new SensorUpdateRequest("4층", null);

        mockMvc.perform(put("/api/v1/sensor/{id}", 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(updateSensorUseCase).updateSensor(1L, 1L, request);
    }

    @Test
    @DisplayName("이름만 수정 성공")
    void 이름만_수정_성공() throws Exception {
        SensorUpdateRequest request = new SensorUpdateRequest(null, "새 이름");

        mockMvc.perform(put("/api/v1/sensor/{id}", 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(updateSensorUseCase).updateSensor(1L, 1L, request);
    }

    @Test
    @DisplayName("이름이 100자를 넘으면 @Valid에 걸려 400, UseCase는 호출 안 됨")
    void 이름_길이초과_400() throws Exception {
        String tooLong = "가".repeat(101);
        SensorUpdateRequest request = new SensorUpdateRequest(null, tooLong);

        mockMvc.perform(put("/api/v1/sensor/{id}", 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(updateSensorUseCase, never()).updateSensor(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("빈 문자열 이름은 @Valid는 통과하지만 서비스 레이어에서 400")
    void 빈이름_서비스레이어에서_거부() throws Exception {
        SensorUpdateRequest request = new SensorUpdateRequest(null, "   ");

        willThrow(new InvalidSensorValueException("변경할 장치 이름은 빈 값일 수 없습니다."))
                .given(updateSensorUseCase).updateSensor(anyLong(), anyLong(), any());

        mockMvc.perform(put("/api/v1/sensor/{id}", 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 위치면 404")
    void 없는_위치_404() throws Exception {
        SensorUpdateRequest request = new SensorUpdateRequest("없는위치", null);

        willThrow(LocationNotFoundException.notFoundLocationByName("없는위치"))
                .given(updateSensorUseCase).updateSensor(anyLong(), anyLong(), any());

        mockMvc.perform(put("/api/v1/sensor/{id}", 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("단일 센서 삭제 성공")
    void 센서_삭제_성공() throws Exception {
        mockMvc.perform(delete("/api/v1/sensor/{id}", 1L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNoContent());

        verify(deleteSensorUseCase).deleteSensor(1L, 1L);
    }

    @Test
    @DisplayName("그룹 전체 센서 삭제 성공")
    void 전체삭제_성공() throws Exception {
        mockMvc.perform(delete("/api/v1/sensor")
                        .header("X-USER-ID", 1L)
                        .param("groupId", "5"))
                .andExpect(status().isNoContent());

        verify(deleteAllSensorUseCase).deleteAll(1L, 5L);
    }

    @Test
    @DisplayName("장소 미배정 센서 목록 조회 성공")
    void 장소_미배정_센서_목록_조회_성공() throws Exception {
        given(getUnassignedSensorsUseCase.getUnassignedSensors(1L, 5L))
                .willReturn(java.util.List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/sensor/unassigned")
                        .header("X-USER-ID", 1L)
                        .param("groupId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sensorId").value(1L));

        verify(getUnassignedSensorsUseCase).getUnassignedSensors(1L, 5L);
    }
}
