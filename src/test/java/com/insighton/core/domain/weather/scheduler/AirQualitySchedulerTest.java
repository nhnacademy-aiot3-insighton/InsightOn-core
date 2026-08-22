package com.insighton.core.domain.weather.scheduler;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.insighton.core.domain.region.dto.GroupRegionDto;
import com.insighton.core.domain.region.dto.RegionGridDto;
import com.insighton.core.domain.region.repository.GroupRegionRepository;
import com.insighton.core.domain.weather.service.WeatherCacheService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AirQualitySchedulerTest {

    @Mock
    private GroupRegionRepository groupRegionRepository;

    @Mock
    private WeatherCacheService weatherCacheService;

    @InjectMocks
    private AirQualityScheduler airQualityScheduler;

    @Test
    @DisplayName("미세먼지 갱신 스케줄러 실행 성공")
    void cacheRefreshScheduler_Success() {
        // given
        RegionGridDto gridDto = new RegionGridDto("서울특별시", "강남구", 60, 127);
        GroupRegionDto groupRegionDto = new GroupRegionDto(1L, gridDto, OffsetDateTime.now());

        given(groupRegionRepository.findAll()).willReturn(List.of(groupRegionDto));

        // when
        airQualityScheduler.cacheRefreshScheduler();

        // then
        verify(weatherCacheService).refreshAirQuality(
                eq(60), eq(127), eq("서울특별시"), eq("강남구"), anyString(), anyString()
        );
    }
}