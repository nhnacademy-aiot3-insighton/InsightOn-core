package com.insighton.core.controller.api;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.insighton.core.domain.weather.service.WeatherCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WeatherController.class)
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean // @MockBean 대체
    private WeatherCacheService weatherCacheService;

    @Test
    @DisplayName("날씨 정보 조회 성공")
    void getWeather_Success() throws Exception {
        // given
        given(weatherCacheService.getWeatherDate(anyInt(), anyInt(), anyString(), anyString(), anyString(),
                anyString()))
                .willReturn(null);

        // when & then
        mockMvc.perform(get("/api/v1/weather") // mockMvc.get -> mockMvc.perform(get(...)) 수정
                        .param("gridX", "60")
                        .param("gridY", "127")
                        .param("sidoName", "서울특별시")
                        .param("cityName", "강남구"))
                .andExpect(status().isOk());
    }
}