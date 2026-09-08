package com.insighton.core.domain.region.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.insighton.core.domain.region.dto.RegionGridDto;
import com.insighton.core.domain.region.exception.RegionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegionCsvParserTest {

    private RegionCsvParser regionCsvParser;

    @BeforeEach
    void setUp() {
        regionCsvParser = new RegionCsvParser();
    }

    @Test
    @DisplayName("정상 CSV 라인 파싱 성공")
    void parse_Success() {
        // given
        String line = "서울특별시,강남구,60,127";

        // when
        RegionGridDto result = regionCsvParser.parse(line);

        // then
        assertThat(result).isNotNull();
        assertThat(result.step1()).isEqualTo("서울특별시");
        assertThat(result.step2()).isEqualTo("강남구");
        assertThat(result.gridX()).isEqualTo(60);
        assertThat(result.gridY()).isEqualTo(127);
    }

    @Test
    @DisplayName("토큰 앞뒤 공백은 trim되어 파싱된다")
    void parse_TrimsWhitespaceAroundTokens() {
        // given
        String line = " 서울특별시 , 강남구 , 60 , 127 ";

        // when
        RegionGridDto result = regionCsvParser.parse(line);

        // then
        assertThat(result.step1()).isEqualTo("서울특별시");
        assertThat(result.step2()).isEqualTo("강남구");
        assertThat(result.gridX()).isEqualTo(60);
        assertThat(result.gridY()).isEqualTo(127);
    }

    @Test
    @DisplayName("4개보다 많은 토큰이 있어도 앞 4개만 사용한다")
    void parse_ExtraTrailingTokens_UsesOnlyFirstFour() {
        // given
        String line = "서울특별시,강남구,60,127,비고컬럼";

        // when
        RegionGridDto result = regionCsvParser.parse(line);

        // then
        assertThat(result).isNotNull();
        assertThat(result.gridX()).isEqualTo(60);
        assertThat(result.gridY()).isEqualTo(127);
    }

    @Test
    @DisplayName("null 라인이면 null을 반환한다")
    void parse_NullLine_ReturnsNull() {
        assertThat(regionCsvParser.parse(null)).isNull();
    }

    @Test
    @DisplayName("공백 라인이면 null을 반환한다")
    void parse_BlankLine_ReturnsNull() {
        assertThat(regionCsvParser.parse("   ")).isNull();
    }

    @Test
    @DisplayName("토큰이 4개 미만이면 null을 반환한다")
    void parse_FewerThanFourTokens_ReturnsNull() {
        // given
        String line = "서울특별시,강남구,60";

        // when & then
        assertThat(regionCsvParser.parse(line)).isNull();
    }

    @Test
    @DisplayName("광역시/도가 비어있으면 예외가 발생한다")
    void parse_BlankStep1_ThrowsException() {
        // given
        String line = ",강남구,60,127";

        // when & then
        assertThatThrownBy(() -> regionCsvParser.parse(line))
                .isInstanceOf(RegionNotFoundException.class);
    }

    @Test
    @DisplayName("시/군/구가 비어있으면 예외가 발생한다")
    void parse_BlankStep2_ThrowsException() {
        // given
        String line = "서울특별시,,60,127";

        // when & then
        assertThatThrownBy(() -> regionCsvParser.parse(line))
                .isInstanceOf(RegionNotFoundException.class);
    }

    @Test
    @DisplayName("격자 좌표가 숫자가 아니면 null을 반환한다")
    void parse_NonNumericGrid_ReturnsNull() {
        // given
        String line = "서울특별시,강남구,abc,127";

        // when & then
        assertThat(regionCsvParser.parse(line)).isNull();
    }
}
