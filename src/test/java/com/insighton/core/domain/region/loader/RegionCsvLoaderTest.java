package com.insighton.core.domain.region.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.insighton.core.domain.region.dto.RegionGridDto;
import com.insighton.core.domain.region.registry.InMemoryRegionRegistry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

/**
 * {@code new ClassPathResource("/data/locations.csv")}가 run() 내부에 하드코딩되어 있어
 * 리소스를 목으로 교체할 수 없음 - 실제 classpath의 locations.csv를 그대로 읽되,
 * regionCsvParser/inMemoryRegionRegistry는 목으로 교체해 호출 횟수/예외 전파만 검증함.
 */
@ExtendWith(MockitoExtension.class)
class RegionCsvLoaderTest {

    @Mock
    private RegionCsvParser regionCsvParser;

    @Mock
    private InMemoryRegionRegistry inMemoryRegionRegistry;

    private RegionCsvLoader regionCsvLoader;

    @BeforeEach
    void setUp() {
        regionCsvLoader = new RegionCsvLoader(regionCsvParser, inMemoryRegionRegistry);
    }

    private static int countDataLines() throws IOException {
        try (InputStream in = RegionCsvLoaderTest.class.getResourceAsStream("/data/locations.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            int count = 0;
            boolean isHeader = true;
            String line;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                count++;
            }
            return count;
        }
    }

    @Test
    @DisplayName("헤더를 제외한 모든 행을 파싱하여 레지스트리에 저장한다")
    void run_Success_ParsesAndSavesEveryDataRow() throws Exception {
        // given
        int dataLineCount = countDataLines();
        RegionGridDto dto = new RegionGridDto("서울특별시", "강남구", 60, 127);
        given(regionCsvParser.parse(anyString())).willReturn(dto);

        // when
        regionCsvLoader.run(mock(ApplicationArguments.class));

        // then - 헤더 한 줄만 건너뛰었다면 parse/save 호출 횟수는 (전체 줄 수 - 1)과 같아야 함
        verify(regionCsvParser, times(dataLineCount)).parse(anyString());
        verify(inMemoryRegionRegistry, times(dataLineCount)).save(dto);
    }

    @Test
    @DisplayName("파싱에 실패한 행이 있으면 즉시 IllegalStateException을 던지고 이후 행은 저장하지 않는다")
    void run_FirstRowParseFails_ThrowsIllegalStateExceptionAndStopsImmediately() {
        // given - 첫 데이터 행에서만 파싱 실패(null)하도록 설정
        given(regionCsvParser.parse(anyString())).willReturn(null);

        // when & then
        assertThatThrownBy(() -> regionCsvLoader.run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잘못된 locations.csv 행");

        verify(regionCsvParser, times(1)).parse(anyString());
        verify(inMemoryRegionRegistry, never()).save(any());
    }
}
