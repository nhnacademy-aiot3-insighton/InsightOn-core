package com.insighton.core.domain.region.registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.insighton.core.domain.region.dto.RegionGridDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryRegionRegistryTest {

    private InMemoryRegionRegistry regionRegistry;

    @BeforeEach
    void setUp() {
        regionRegistry = new InMemoryRegionRegistry();
    }

    @Test
    @DisplayName("지역 정보 저장 및 단건 격자 좌표 조회 성공")
    void saveAndFindGridCoordinate_Success() {
        // given
        RegionGridDto gridDto = new RegionGridDto("서울특별시", "강남구", 60, 127);

        // when
        regionRegistry.save(gridDto);
        Optional<RegionGridDto> result = regionRegistry.findGridCoordinate("서울특별시", "강남구");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().gridX()).isEqualTo(60);
        assertThat(result.get().gridY()).isEqualTo(127);
    }

    @Test
    @DisplayName("시/도 목록 조회 성공 (중복 제거 및 등록 확인)")
    void findAllStates_Success() {
        // given
        regionRegistry.save(new RegionGridDto("서울특별시", "강남구", 60, 127));
        regionRegistry.save(new RegionGridDto("서울특별시", "서초구", 61, 126));
        regionRegistry.save(new RegionGridDto("부산광역시", "해운대구", 98, 75));

        // when
        List<String> states = regionRegistry.findAllStates();

        // then
        assertThat(states).hasSize(2)
                .containsExactlyInAnyOrder("서울특별시", "부산광역시");
    }

    @Test
    @DisplayName("특정 시/도의 시/군/구 목록 조회 성공")
    void findCitiesByState_Success() {
        // given
        regionRegistry.save(new RegionGridDto("서울특별시", "강남구", 60, 127));
        regionRegistry.save(new RegionGridDto("서울특별시", "서초구", 61, 126));

        // when
        List<String> cities = regionRegistry.findCitiesByState("서울특별시");

        // then
        assertThat(cities).hasSize(2)
                .containsExactlyInAnyOrder("강남구", "서초구");
    }

    @Test
    @DisplayName("존재하지 않는 시/도 조회 시 빈 리스트 반환")
    void findCitiesByState_NotFound_ReturnsEmptyList() {
        // when
        List<String> cities = regionRegistry.findCitiesByState("존재하지않는시");

        // then
        assertThat(cities).isEmpty();
    }

    @Test
    @DisplayName("초기화(Clear) 동작 검증")
    void clear_Success() {
        // given
        regionRegistry.save(new RegionGridDto("서울특별시", "강남구", 60, 127));

        // when
        regionRegistry.clear();

        // then
        assertThat(regionRegistry.findAllStates()).isEmpty();
        assertThat(regionRegistry.findGridCoordinate("서울특별시", "강남구")).isEmpty();
    }
}