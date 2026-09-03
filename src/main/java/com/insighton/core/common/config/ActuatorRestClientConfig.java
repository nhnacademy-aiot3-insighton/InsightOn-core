package com.insighton.core.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    // LG ThinQ Connect 필수 헤더 (docs/provider-contract.md §2)
    //  api-key   : 실제로는 SDK에 내장된 공개 고정키
    //  client-id : 우리 앱을 식별하는 고정 UUID (요청마다 바뀌는 x-message-id 와 다름)
    @Value("${actuator.lg-thinq.api-key}")
    private String lgThinQApiKey;

    @Value("${actuator.lg-thinq.client-id}")
    private String lgThinQClientId;

    @Value("${actuator.lg-thinq.country:KR}")
    private String lgThinQCountry;

    // SmartThings 호출용: base-url + Authorization: Bearer
    @Bean
    public RestClient smartThingsRestClient() {
        return RestClient.builder()
                .baseUrl(smartThingsBaseUrl)
                .defaultHeader("Authorization", "Bearer " + smartThingsToken)
                .build();
    }

    // LG ThinQ 호출용: base-url + 매 요청 공통 필수 헤더 (x-message-id는 요청마다 달라 여기 없음)
    @Bean
    public RestClient lgThinQRestClient() {
        return RestClient.builder()
                .baseUrl(lgThinQBaseUrl)
                .defaultHeader("Authorization", "Bearer " + lgThinQToken)
                .defaultHeader("x-api-key", lgThinQApiKey)
                .defaultHeader("x-client-id", lgThinQClientId)
                .defaultHeader("x-country", lgThinQCountry)
                .defaultHeader("x-service-phase", "OP")
                .build();
    }
}
