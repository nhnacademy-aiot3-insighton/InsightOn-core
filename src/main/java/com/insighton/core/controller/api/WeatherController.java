package com.insighton.core.controller.api;

import com.insighton.core.domain.weather.dto.WeatherDataDto;
import com.insighton.core.domain.weather.service.WeatherCacheService;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/weather")
public class WeatherController {

    private final WeatherCacheService weatherCacheService;

    @GetMapping
    public ResponseEntity<WeatherDataDto> getWeather(
            @RequestParam int gridX,
            @RequestParam int gridY,
            @RequestParam String sidoName,
            @RequestParam String cityName
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = now.format(DateTimeFormatter.ofPattern("HHmm"));

        WeatherDataDto weatherDataDto = weatherCacheService.getWeatherDate(
                gridX, gridY, sidoName, cityName, baseDate, baseTime);

        return ResponseEntity.ok(weatherDataDto);
    }
}
