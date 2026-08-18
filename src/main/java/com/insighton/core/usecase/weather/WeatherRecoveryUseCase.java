package com.insighton.core.usecase.weather;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.region.dto.GroupRegionDto;
import com.insighton.core.domain.region.service.RegionService;
import com.insighton.core.domain.weather.dto.WeatherDataDto;
import com.insighton.core.domain.weather.exception.WeatherApiException;
import com.insighton.core.domain.weather.service.WeatherCacheService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class  WeatherRecoveryUseCase {

    private final RegionService regionService;
    private final WeatherCacheService weatherCacheService;

    public WeatherDataDto recoveryWeather(Long groupId, String baseDate, String baseTime) {

        GroupRegionDto groupRegionDto = regionService.findByGroupRegion(groupId)
                .orElseThrow(() -> new WeatherApiException("그룹 지역 정보를 찾을 수 없습니다. (데이터 유실 상태)"));

        int gridX = groupRegionDto.regionGridDto().gridX();
        int gridY = groupRegionDto.regionGridDto().gridY();
        String sidoName = groupRegionDto.regionGridDto().step1();
        String cityName = groupRegionDto.regionGridDto().step2();

        return weatherCacheService.getWeatherDate(gridX, gridY, sidoName, cityName, baseDate, baseTime);
    }
}
