package com.insighton.core.weather.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class WeatherClientConfig {

    @Value("${weather.api.kma-base-url}")
    private String kmaBaseUrl;

    @Value("${weather.api.air-base-url}")
    private String airBaseUrl;

    @Bean
    public RestClient kmaRestClient() {
        return RestClient.builder()
                .baseUrl(kmaBaseUrl)
                .build();
    }

    @Bean
    public RestClient airQualityRestClient() {
        return RestClient.builder()
                .baseUrl(airBaseUrl)
                .build();
    }
}
