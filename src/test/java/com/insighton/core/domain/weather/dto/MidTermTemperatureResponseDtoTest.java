package com.insighton.core.domain.weather.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class MidTermTemperatureResponseDtoTest {

    // data.go.kr getMidTa 실제 응답 - 숫자 필드(taMinN/taMaxN)가 JSON number로 내려오므로
    // String 타입 record 필드로 정상 역직렬화되는지 검증 (2026-09-03 서울/11B10101, 06시 발표분)
    private static final String SAMPLE_RESPONSE = """
            {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},"body":{"dataType":"JSON",
            "items":{"item":[{"regId":"11B10101","taMin4":21,"taMin4Low":1,"taMin4High":2,"taMax4":29,
            "taMax4Low":1,"taMax4High":1,"taMin10":19,"taMax10":26}]},"pageNo":1,"numOfRows":10,"totalCount":1}}}
            """;

    @Test
    void 숫자로_내려오는_기온_필드가_String으로_정상_파싱된다() throws Exception {
        MidTermTemperatureResponseDto response = new ObjectMapper().readValue(SAMPLE_RESPONSE,
                MidTermTemperatureResponseDto.class);

        MidTermTemperatureResponseDto.Item item = response.response().body().items().item().get(0);

        assertThat(item.regId()).isEqualTo("11B10101");
        assertThat(item.taMin4()).isEqualTo("21");
        assertThat(item.taMax4()).isEqualTo("29");
        assertThat(item.taMin10()).isEqualTo("19");
        assertThat(item.taMax10()).isEqualTo("26");
    }
}
