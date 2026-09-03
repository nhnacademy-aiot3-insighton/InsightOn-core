package com.insighton.core.common.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// 액추에이터 공급자(SmartThings / LG ThinQ) 호출용 RestClient 빈.
// base-url + 공통 인증 헤더를 미리 박아둔다. 실연동 전환 = properties의 base-url·토큰·키만 교체 (코드 0).
//
//  실제 base-url
//   - SmartThings : https://api.smartthings.com
//   - LG ThinQ    : https://api-kic.lgthinq.com (KR) / api-aic (US) / api-eic (EU)
//  로컬은 두 값 모두 actuator-simulator(8090)를 가리킨다.
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

    // LG ThinQ Connect 필수 헤더
    //  api-key   : 실제로는 SDK에 내장된 공개 고정키
    //  client-id : 우리 앱을 식별하는 고정 UUID (요청마다 바뀌는 x-message-id 와 다름)
    @Value("${actuator.lg-thinq.api-key}")
    private String lgThinQApiKey;

    @Value("${actuator.lg-thinq.client-id}")
    private String lgThinQClientId;

    @Value("${actuator.lg-thinq.country:KR}")
    private String lgThinQCountry;

    // 공급자 호출이 무한정 매달리지 않도록 연결 3초 / 응답 10초 타임아웃 (동기 요청 스레드 보호)
    private static ClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }

    // SmartThings 호출용: base-url + Authorization: Bearer
    @Bean
    public RestClient smartThingsRestClient() {
        return RestClient.builder()
                .requestFactory(timeoutRequestFactory())
                .baseUrl(smartThingsBaseUrl)
                .defaultHeader("Authorization", "Bearer " + smartThingsToken)
                .build();
    }

    // LG ThinQ 호출용: base-url + 매 요청 공통 필수 헤더 (x-message-id는 요청마다 달라 여기 없음)
    @Bean
    public RestClient lgThinQRestClient() {
        return RestClient.builder()
                .requestFactory(timeoutRequestFactory())
                .baseUrl(lgThinQBaseUrl)
                .defaultHeader("Authorization", "Bearer " + lgThinQToken)
                .defaultHeader("x-api-key", lgThinQApiKey)
                .defaultHeader("x-client-id", lgThinQClientId)
                .defaultHeader("x-country", lgThinQCountry)
                .defaultHeader("x-service-phase", "OP")
                .build();
    }
}
