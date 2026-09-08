package com.insighton.core.adapter.client.external;

import com.insighton.core.domain.weather.dto.AirQualityResponseDto;
import com.insighton.core.domain.weather.exception.WeatherApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AirQualityApiClientTest {

    @Mock
    private RestClient airQualityRestClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private AirQualityApiClient airQualityApiClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(airQualityApiClient, "airBaseUrl", "http://apis.data.go.kr");
        ReflectionTestUtils.setField(airQualityApiClient, "airApiKey", "testAirKey");
    }

    @Test
    @DisplayName("에어코리아 대기질 데이터 파싱 및 평균 계산 성공 케이스")
    void fetchAirQualityData_success() {
        // given
        AirQualityResponseDto.Item item1 = new AirQualityResponseDto.Item("Station1", "강남구", "10", "20", "15", "25", "1", "2");
        AirQualityResponseDto.Item item2 = new AirQualityResponseDto.Item("Station2", "강남구", "30", "40", "35", "45", "3", "4");

        AirQualityResponseDto.Header header = new AirQualityResponseDto.Header("00", "OK");
        AirQualityResponseDto.Body body = new AirQualityResponseDto.Body(List.of(item1, item2), 2);
        AirQualityResponseDto response = new AirQualityResponseDto(new AirQualityResponseDto.Response(header, body));

        given(airQualityRestClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(any(URI.class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(AirQualityResponseDto.class)).willReturn(response);

        // when
        Map<String, String> result = airQualityApiClient.fetchAirQualityData("서울", "강남구");

        // then
        assertThat(result)
                .containsEntry("pm10Value", "20.0") // (10+30)/2
                .containsEntry("pm25Value", "30.0") // (20+40)/2
                .containsEntry("pm10Grade1h", "3")
                .containsEntry("pm25Grade1h", "4");
    }

    @Test
    @DisplayName("유효하지 않은 수치('-') 포함 시 계산 제외 및 N/A 처리")
    void fetchAirQualityData_invalidValue_handled() {
        // given
        AirQualityResponseDto.Item item1 = new AirQualityResponseDto.Item("Station1", "강남구", "-", "-", "-", "-", "-", "-");
        AirQualityResponseDto.Header header = new AirQualityResponseDto.Header("00", "OK");
        AirQualityResponseDto.Body body = new AirQualityResponseDto.Body(List.of(item1), 1);
        AirQualityResponseDto response = new AirQualityResponseDto(new AirQualityResponseDto.Response(header, body));

        given(airQualityRestClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(any(URI.class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(AirQualityResponseDto.class)).willReturn(response);

        // when
        Map<String, String> result = airQualityApiClient.fetchAirQualityData("서울", "강남구");

        // then
        assertThat(result).containsEntry("pm10Value", "N/A");
        assertThat(result.get("pm10Grade1h")).isNull();
    }

    @Test
    @DisplayName("API 호출 중 예외 발생 시 WeatherApiException 예외 전환")
    void fetchAirQualityData_exception_throwsWeatherApiException() {
        // given
        given(airQualityRestClient.get()).willThrow(new RuntimeException("Connection Timeout"));

        // when & then
        assertThatThrownBy(() -> airQualityApiClient.fetchAirQualityData("서울", "강남구"))
                .isInstanceOf(WeatherApiException.class);
    }
}
