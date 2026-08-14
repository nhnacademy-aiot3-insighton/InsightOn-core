package com.insighton.core.domain.weather.scheduler;

import com.insighton.core.domain.region.dto.GroupRegionDto;
import com.insighton.core.domain.region.repository.GroupRegionRepository;
import com.insighton.core.domain.weather.service.WeatherCacheService;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrentForecastWeatherScheduler {

    private final GroupRegionRepository groupRegionRepository;
    private final WeatherCacheService weatherCacheService;

    @Scheduled(cron = "0 40 * * * *") //초단기실황.예보시각 + 10분
    public void cacheRefreshScheduler() {
        log.info("[스케줄러 시작] 초단기실황 및 초단기예보 데이터 갱신");

        OffsetDateTime now = OffsetDateTime.now(ZoneId.systemDefault());
        String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = now.format(DateTimeFormatter.ofPattern("HHmm"));
        
        List<GroupRegionDto> allRegions = groupRegionRepository.findAll();

        for (GroupRegionDto regionDto : allRegions) {
            int gridX = regionDto.regionGridDto().gridX();
            int gridY = regionDto.regionGridDto().gridY();

            try {
                weatherCacheService.refreshCurrentWeather(gridX, gridY, baseDate, baseTime);
                log.info("[초단기 갱신 성공] X: {}, Y: {}", gridX, gridY);
            } catch (Exception e) {
                log.error("[초단기 갱신 실패] X: {}, Y; {}, 원인: {}", gridX, gridY, e.getMessage());
            }
        }
        log.info("[스케줄러 종료] 초단기실황 및 초단기예보 데이터 갱신 완료");
    }
}
