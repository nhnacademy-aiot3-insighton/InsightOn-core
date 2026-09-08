package com.insighton.core.domain.weather.scheduler;

import com.insighton.core.domain.region.dto.GroupRegionDto;
import com.insighton.core.domain.region.dto.RegionGridDto;
import com.insighton.core.domain.region.repository.GroupRegionRepository;
import com.insighton.core.domain.weather.service.WeatherCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CurrentForecastWeatherSchedulerTest {

    @Mock
    private GroupRegionRepository groupRegionRepository;

    @Mock
    private WeatherCacheService weatherCacheService;

    @InjectMocks
    private CurrentForecastWeatherScheduler currentForecastWeatherScheduler;

    @Test
    @DisplayName("초단기실황 스케줄러 실행 성공")
    void cacheRefreshScheduler_success() {
        // given
        RegionGridDto gridDto = new RegionGridDto("서울특별시", "강남구", 60, 127);
        GroupRegionDto regionDto = new GroupRegionDto(1L, gridDto, OffsetDateTime.now());
        given(groupRegionRepository.findAll()).willReturn(List.of(regionDto));

        // when
        currentForecastWeatherScheduler.cacheRefreshScheduler();

        // then
        verify(weatherCacheService).refreshCurrentWeather(anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("초단기실황 스케줄러 예외 처리 동작")
    void cacheRefreshScheduler_handlesException() {
        // given
        RegionGridDto gridDto = new RegionGridDto("서울특별시", "강남구", 60, 127);
        GroupRegionDto regionDto = new GroupRegionDto(1L, gridDto, OffsetDateTime.now());
        given(groupRegionRepository.findAll()).willReturn(List.of(regionDto));

        doThrow(new RuntimeException("Current weather error"))
                .when(weatherCacheService).refreshCurrentWeather(anyInt(), anyInt(), anyString(), anyString());

        // when
        currentForecastWeatherScheduler.cacheRefreshScheduler();

        // then
        verify(weatherCacheService).refreshCurrentWeather(anyInt(), anyInt(), anyString(), anyString());
    }
}
