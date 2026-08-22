package com.insighton.core.domain.region.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.insighton.core.domain.region.dto.GroupRegionDto;
import com.insighton.core.domain.region.dto.RegionGridDto;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryGroupRegionRepositoryTest {

    private InMemoryGroupRegionRepository groupRegionRepository;

    @BeforeEach
    void setUp() {
        groupRegionRepository = new InMemoryGroupRegionRepository();
    }

    @Test
    @DisplayName("그룹 지역 정보 저장 및 그룹 ID로 조회 성공")
    void saveAndFindByGroupId_Success() {
        // given
        Long groupId = 1L;
        RegionGridDto gridDto = new RegionGridDto("서울특별시", "강남구", 60, 127);
        GroupRegionDto groupRegionDto = new GroupRegionDto(groupId, gridDto, OffsetDateTime.now());

        // when
        groupRegionRepository.save(groupRegionDto);
        Optional<GroupRegionDto> result = groupRegionRepository.findByGroupId(groupId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().groupId()).isEqualTo(groupId);
        assertThat(result.get().regionGridDto().step1()).isEqualTo("서울특별시");
    }

    @Test
    @DisplayName("전체 그룹 지역 정보 목록 조회 성공")
    void findAll_Success() {
        // given
        RegionGridDto gridDto = new RegionGridDto("서울특별시", "강남구", 60, 127);
        groupRegionRepository.save(new GroupRegionDto(1L, gridDto, OffsetDateTime.now()));
        groupRegionRepository.save(new GroupRegionDto(2L, gridDto, OffsetDateTime.now()));

        // when
        List<GroupRegionDto> all = groupRegionRepository.findAll();

        // then
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("그룹 ID로 지역 정보 삭제 성공")
    void deleteByGroupId_Success() {
        // given
        Long groupId = 1L;
        RegionGridDto gridDto = new RegionGridDto("서울특별시", "강남구", 60, 127);
        groupRegionRepository.save(new GroupRegionDto(groupId, gridDto, OffsetDateTime.now()));

        // when
        groupRegionRepository.deleteByGroupId(groupId);
        Optional<GroupRegionDto> result = groupRegionRepository.findByGroupId(groupId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("전체 데이터 초기화(Clear) 성공")
    void clear_Success() {
        // given
        RegionGridDto gridDto = new RegionGridDto("서울특별시", "강남구", 60, 127);
        groupRegionRepository.save(new GroupRegionDto(1L, gridDto, OffsetDateTime.now()));

        // when
        groupRegionRepository.clear();

        // then
        assertThat(groupRegionRepository.findAll()).isEmpty();
    }
}