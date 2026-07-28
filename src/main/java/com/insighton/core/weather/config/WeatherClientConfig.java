package com.insighton.core.weather.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WeatherClientConfig {

    @Value("${weather.api.kma-base-url}")
    private String kmaBaseUrl;

    @Value("${weather.api.air-base-url}")
    private String airBaseUrl;

    @Bean
    public WebClient kmaWebClient() {
        return WebClient.builder()
                .baseUrl(kmaBaseUrl)
                .build();
    }

    @Bean
    public WebClient airQualityWebClient() {
        return WebClient.builder()
                .baseUrl(airBaseUrl)
                .build();
    }
}
