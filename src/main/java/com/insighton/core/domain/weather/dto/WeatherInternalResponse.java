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
        Double forecastHumidity,
        Double midTermAvgMaxTemp, // 4~10일 후 평균 최고기온 전망
        Double midTermAvgMinTemp  // 4~10일 후 평균 최저기온 전망
) {
    public static WeatherInternalResponse from(WeatherDataDto weatherDataDto) {
        CurrentWeatherDto current = weatherDataDto.current();
        ForecastWeatherDto forecast = weatherDataDto.forecast();
        AirQualityDto airQuality = weatherDataDto.airQuality();
        MidTermTemperatureDto midTerm = weatherDataDto.midTermTemperature();

        return new WeatherInternalResponse(
                current != null ? parseDoubleOrNull(current.temp()) : null,
                forecast != null ? forecast.skyStatus() : null,
                current != null ? current.precipitationType() : null,
                current != null ? parseDoubleOrNull(current.humidity()) : null,
                forecast != null ? parseDoubleOrNull(forecast.maxTemp()) : null,
                forecast != null ? parseDoubleOrNull(forecast.minTemp()) : null,
                airQuality != null ? airQuality.pm10Grade() : null,
                forecast != null ? parseDoubleOrNull(forecast.temperature()) : null,
                forecast != null ? forecast.skyStatus() : null,
                forecast != null ? forecast.forecastPrecipitationType() : null,
                null,
                midTerm != null ? parseDoubleOrNull(midTerm.avgMaxTemp()) : null,
                midTerm != null ? parseDoubleOrNull(midTerm.avgMinTemp()) : null
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