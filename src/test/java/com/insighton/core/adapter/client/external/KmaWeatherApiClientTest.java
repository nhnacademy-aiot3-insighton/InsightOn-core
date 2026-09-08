package com.insighton.core.adapter.client.external;

import com.insighton.core.domain.weather.dto.KmaWeatherResponseDto;
import com.insighton.core.domain.weather.dto.MidTermTemperatureResponseDto;
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
class KmaWeatherApiClientTest {

    @Mock
    private RestClient kmaRestClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private KmaWeatherApiClient kmaWeatherApiClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kmaWeatherApiClient, "kmaBaseUrl", "http://apis.data.go.kr");
        ReflectionTestUtils.setField(kmaWeatherApiClient, "kmaApiKey", "testKey");
        ReflectionTestUtils.setField(kmaWeatherApiClient, "kmaMidtermBaseUrl", "http://apis.data.go.kr");
        ReflectionTestUtils.setField(kmaWeatherApiClient, "kmaMidtermApiKey", "testMidKey");
    }

    @Test
    @DisplayName("초단기실황(isNcst=true) 조회 성공 케이스")
    void fetchKmaApi_ncst_success() {
        // given
        KmaWeatherResponseDto.Item item1 = new KmaWeatherResponseDto.Item("20260908", "0600", "T1H", "20260908", "0600", "25.0", "25.0", 60, 127);
        KmaWeatherResponseDto.Item item2 = new KmaWeatherResponseDto.Item("20260908", "0600", "REH", "20260908", "0600", "60", "60", 60, 127);
        KmaWeatherResponseDto.Body body = new KmaWeatherResponseDto.Body("JSON", new KmaWeatherResponseDto.Items(List.of(item1, item2)), 1, 100, 2);
        KmaWeatherResponseDto.Header header = new KmaWeatherResponseDto.Header("00", "OK");
        KmaWeatherResponseDto response = new KmaWeatherResponseDto(new KmaWeatherResponseDto.Response(header, body));

        given(kmaRestClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(any(URI.class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(KmaWeatherResponseDto.class)).willReturn(response);

        // when
        Map<String, String> result = kmaWeatherApiClient.fetchKmaApi("/endpoint", 60, 127, "20260908", "0600", true);

        // then
        assertThat(result).containsEntry("T1H", "25.0").containsEntry("REH", "60");
    }

    @Test
    @DisplayName("단기예보(isNcst=false) 및 TMX/TMN 처리 성공 케이스")
    void fetchKmaApi_forecast_success() {
        // given
        KmaWeatherResponseDto.Item item1 = new KmaWeatherResponseDto.Item("20260908", "0600", "TMP", "20260908", "1200", "22", null, 60, 127);
        KmaWeatherResponseDto.Item item2 = new KmaWeatherResponseDto.Item("20260908", "0600", "TMX", "20260908", "1500", "28", null, 60, 127);
        KmaWeatherResponseDto.Body body = new KmaWeatherResponseDto.Body("JSON", new KmaWeatherResponseDto.Items(List.of(item1, item2)), 1, 100, 2);
        KmaWeatherResponseDto.Header header = new KmaWeatherResponseDto.Header("00", "OK");
        KmaWeatherResponseDto response = new KmaWeatherResponseDto(new KmaWeatherResponseDto.Response(header, body));

        given(kmaRestClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(any(URI.class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(KmaWeatherResponseDto.class)).willReturn(response);

        // when
        Map<String, String> result = kmaWeatherApiClient.fetchKmaApi("/endpoint", 60, 127, "20260908", "0600", false);

        // then
        assertThat(result).containsEntry("TMP", "22").containsEntry("TMX", "28");
    }

    @Test
    @DisplayName("기상청 API 응답이 null이거나 구조가 없으면 예외 발생")
    void fetchKmaApi_nullResponse_throwsException() {
        // given
        given(kmaRestClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(any(URI.class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(KmaWeatherResponseDto.class)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> kmaWeatherApiClient.fetchKmaApi("/endpoint", 60, 127, "20260908", "0600", true))
                .isInstanceOf(WeatherApiException.class);
    }

    @Test
    @DisplayName("API 호출 중 예외가 터지면 WeatherApiException 예외 전환")
    void fetchKmaApi_clientException_throwsException() {
        // given
        given(kmaRestClient.get()).willThrow(new RuntimeException("Connection Failed"));

        // when & then
        assertThatThrownBy(() -> kmaWeatherApiClient.fetchKmaApi("/endpoint", 60, 127, "20260908", "0600", true))
                .isInstanceOf(WeatherApiException.class);
    }

    @Test
    @DisplayName("중기기온조회 성공 케이스")
    void fetchMidTermTemperature_success() {
        // given
        MidTermTemperatureResponseDto.Item item = new MidTermTemperatureResponseDto.Item(
                "11B10101", "15", "25", "15", "25", "15", "25", "15", "25", "15", "25", "15", "25", "15", "25"
        );
        MidTermTemperatureResponseDto.Body body = new MidTermTemperatureResponseDto.Body(new MidTermTemperatureResponseDto.Items(List.of(item)));
        MidTermTemperatureResponseDto response = new MidTermTemperatureResponseDto(new MidTermTemperatureResponseDto.Response(null, body));

        given(kmaRestClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(any(URI.class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(MidTermTemperatureResponseDto.class)).willReturn(response);

        // when
        MidTermTemperatureResponseDto.Item result = kmaWeatherApiClient.fetchMidTermTemperature("11B10101", "202609080600");

        // then
        assertThat(result.regId()).isEqualTo("11B10101");
        assertThat(result.taMin4()).isEqualTo("15");
        assertThat(result.taMax4()).isEqualTo("25");
    }
}
