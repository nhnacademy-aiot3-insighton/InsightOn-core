package com.insighton.core.domain.weather.dto;

/**
 * AI 서비스 내부 API(GET /internal/v1/groups/{group-id}/weather) 전용 응답.
 * WeatherDataDto를 AI가 기대하는 평평한 구조로 변환
 */
public record WeatherInternalResponse(
        Double temperature,
        String skyStatus,
        String precipitationType,
        Double humidity,
        Double maxTemp,
        Double minTemp,
        String dustGrade,
        Double forecastTemperature,
        String forecastSkyStatus,
        String forecastPrecipitationType,
        Double forecastHumidity
) {
    public static WeatherInternalResponse from(WeatherDataDto weatherDataDto) {
        CurrentWeatherDto current = weatherDataDto.current();
        ForecastWeatherDto forecast = weatherDataDto.forecast();
        AirQualityDto airQuality = weatherDataDto.airQuality();

        return new WeatherInternalResponse(
                parseDoubleOrNull(current.temp()),
                // 초단기실황엔 하늘상태 필드가 없어 단기예보값으로 대체(현재/예보 동일값)
                forecast.skyStatus(),
                current.precipitationType(),
                parseDoubleOrNull(current.humidity()),
                parseDoubleOrNull(forecast.maxTemp()),
                parseDoubleOrNull(forecast.minTemp()),
                airQuality.pm10Grade(),
                parseDoubleOrNull(forecast.temperature()),
                forecast.skyStatus(),
                forecast.forecastPrecipitationType(),
                // Core에 예보 습도 데이터 소스가 없어 null
                null
        );
    }

    private static Double parseDoubleOrNull(String value) {
        try {
            return value != null ? Double.parseDouble(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}