package com.insighton.core.weather.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WeatherClientConfig {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Bean
    public WebClient kmaWebClient() {
        return WebClient.builder()
                .baseUrl("https://apihub.kma.go.kr/api/typ01/cgi-bin/url")
                .clientConnector(new JdkClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public WebClient airQualityWebClient() {
        return WebClient.builder()
                .baseUrl("https://apis.data.go.kr/B552584/ArpltnInforInqireSvc")
                .clientConnector(new JdkClientHttpConnector(httpClient))
                .build();
    }
}
