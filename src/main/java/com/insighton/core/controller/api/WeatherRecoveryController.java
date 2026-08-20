package com.insighton.core.controller.api;

import com.insighton.core.domain.weather.dto.WeatherDataDto;
import com.insighton.core.usecase.weather.WeatherRecoveryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/weather")
public class WeatherRecoveryController {

    private final WeatherRecoveryUseCase weatherRecoveryUseCase;

    @GetMapping("/group/{groupId}")
    public ResponseEntity<WeatherDataDto> getWeatherByGroupId(
            @PathVariable Long groupId,
            @RequestParam String baseDate,
            @RequestParam String baseTime
    ) {
        WeatherDataDto weatherDataDto = weatherRecoveryUseCase.recoveryWeather(groupId, baseDate, baseTime);

        return ResponseEntity.ok(weatherDataDto);
    }
}
