package com.insighton.core.controller.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.insighton.core.domain.region.service.RegionService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RegionController.class)
class RegionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean // @MockBean 대체
    private RegionService regionService;

    @Test
    @DisplayName("광역시/도 목록 조회 성공")
    void getAllStates_Success() throws Exception {
        // given
        given(regionService.getSortedStates()).willReturn(List.of("서울특별시", "부산광역시"));

        // when & then
        mockMvc.perform(get("/api/v1/regions/states")) // mockMvc.get -> mockMvc.perform(get(...)) 수정
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("서울특별시"))
                .andExpect(jsonPath("$[1]").value("부산광역시"));
    }

    @Test
    @DisplayName("시/군/구 목록 조회 성공")
    void getCitiesByState_Success() throws Exception {
        // given
        given(regionService.getSortedCities(anyString())).willReturn(List.of("강남구", "서초구"));

        // when & then
        mockMvc.perform(get("/api/v1/regions/cities") // mockMvc.get -> mockMvc.perform(get(...)) 수정
                        .param("state", "서울특별시"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("강남구"))
                .andExpect(jsonPath("$[1]").value("서초구"));
    }
}