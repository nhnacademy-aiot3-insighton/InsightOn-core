package com.insighton.core.controller.internal;

import com.insighton.core.domain.weather.dto.WeatherDataDto;
import com.insighton.core.domain.weather.dto.WeatherInternalResponse;
import com.insighton.core.usecase.WeatherRecoveryUseCase;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WeatherInternalController {

    private final WeatherRecoveryUseCase weatherRecoveryUseCase;

    @GetMapping("/groups/{group-id}/weather")
    public ResponseEntity<WeatherInternalResponse> getWeather(@PathVariable("group-id") Long groupId) {

        OffsetDateTime now = OffsetDateTime.now();

        String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = now.format(DateTimeFormatter.ofPattern("HHmm"));

        WeatherDataDto weatherDataDto = weatherRecoveryUseCase.recoveryWeather(groupId, baseDate, baseTime);

        return ResponseEntity.ok(WeatherInternalResponse.from(weatherDataDto));
    }
}
