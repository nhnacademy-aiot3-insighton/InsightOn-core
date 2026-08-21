package com.insighton.core.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.insighton.core.domain.region.dto.GroupRegionDto;
import com.insighton.core.domain.region.dto.RegionGridDto;
import com.insighton.core.domain.region.service.RegionService;
import com.insighton.core.domain.weather.exception.WeatherApiException;
import com.insighton.core.domain.weather.service.WeatherCacheService;
import com.insighton.core.usecase.weather.WeatherRecoveryUseCase;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeatherRecoveryUseCaseTest {

    @Mock
    private RegionService regionService;

    @Mock
    private WeatherCacheService weatherCacheService;

    @InjectMocks
    private WeatherRecoveryUseCase weatherRecoveryUseCase;

    @Test
    @DisplayName("그룹 ID로 날씨 복구 성공")
    void recoveryWeather_Success() {
        // given
        Long groupId = 1L;
        RegionGridDto gridDto = new RegionGridDto("서울특별시", "강남구", 60, 127);
        GroupRegionDto groupRegionDto = new GroupRegionDto(groupId, gridDto, OffsetDateTime.now());

        given(regionService.findByGroupRegion(groupId)).willReturn(Optional.of(groupRegionDto));

        // when
        weatherRecoveryUseCase.recoveryWeather(groupId, "20260601", "1200");

        // then
        verify(weatherCacheService).getWeatherDate(eq(60), eq(127), eq("서울특별시"), eq("강남구"), eq("20260601"), eq("1200"));
    }

    @Test
    @DisplayName("그룹 지역 정보가 없는 경우 예외 발생")
    void recoveryWeather_NotFound_ThrowsException() {
        // given
        Long groupId = 99L;
        given(regionService.findByGroupRegion(groupId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> weatherRecoveryUseCase.recoveryWeather(groupId, "20260601", "1200"))
                .isInstanceOf(WeatherApiException.class)
                .hasMessageContaining("그룹 지역 정보를 찾을 수 없습니다.");
    }
}