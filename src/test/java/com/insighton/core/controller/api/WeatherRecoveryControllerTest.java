package com.insighton.core.controller.api;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.insighton.core.usecase.WeatherRecoveryUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WeatherRecoveryController.class)
class WeatherRecoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeatherRecoveryUseCase weatherRecoveryUseCase;

    @Test
    @DisplayName("그룹 ID를 통한 날씨 복구 조회 성공")
    void getWeatherByGroupId_Success() throws Exception {
        // given
        Long groupId = 1L;
        given(weatherRecoveryUseCase.recoveryWeather(anyLong(), anyString(), anyString()))
                .willReturn(null); // 필요한 경우 Mock WeatherDataDto 객체 반환 설정 가능

        // when & then
        mockMvc.perform(get("/api/v1/weather/group/{groupId}", groupId)
                        .param("baseDate", "20260601")
                        .param("baseTime", "1200"))
                .andExpect(status().isOk());
    }
}