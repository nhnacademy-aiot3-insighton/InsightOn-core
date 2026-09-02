package com.insighton.core.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// 액추에이터 공급자(SmartThings/LG ThinQ) 호출용 RestClient 빈.
// 기존 RestClientConfig(kma/airQuality)와 동일하게 공급자별 base-url + 인증 헤더를 미리 박아둔다.
@Configuration
public class ActuatorRestClientConfig {

    @Value("${actuator.smartthings.base-url}")
    private String smartThingsBaseUrl;

    @Value("${actuator.smartthings.token}")
    private String smartThingsToken;

    @Value("${actuator.lg-thinq.base-url}")
    private String lgThinQBaseUrl;

    @Value("${actuator.lg-thinq.token}")
    private String lgThinQToken;

    @Bean
    public RestClient smartThingsRestClient() {
        return RestClient.builder()
                .baseUrl(smartThingsBaseUrl)
                .defaultHeader("Authorization", "Bearer " + smartThingsToken)
                .build();
    }

    @Bean
    public RestClient lgThinQRestClient() {
        return RestClient.builder()
                .baseUrl(lgThinQBaseUrl)
                .defaultHeader("Authorization", "Bearer " + lgThinQToken)
                .build();
    }
}
