package com.insighton.core.region.service;

import com.insighton.core.domain.region.dto.GroupRegionDto;
import com.insighton.core.domain.region.dto.RegionGridDto;
import com.insighton.core.domain.region.exception.RegionNotFoundException;
import com.insighton.core.domain.region.registry.RegionRegistry;
import com.insighton.core.domain.region.repository.GroupRegionRepository;
import com.insighton.core.domain.region.service.RegionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    @Mock
    private RegionRegistry regionRegistry;

    @Mock
    private GroupRegionRepository groupRegionRepository;

    @InjectMocks
    private RegionService regionService;

    private static final Long GROUP_ID = 1L;
    private static final RegionGridDto GRID_DTO = new RegionGridDto("Seoul", "Jongno", 60, 127);

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("행정구역 검증 성공")
        void validateRegion_success() {
            // given
            given(regionRegistry.findGridCoordinate("Seoul", "Jongno")).willReturn(Optional.of(GRID_DTO));

            // when
            RegionGridDto result = regionService.validateRegion("Seoul", "Jongno");

            // then
            assertThat(result).isEqualTo(GRID_DTO);
        }

        @Test
        @DisplayName("groupId 좌표 캐싱 성공 - state,city 문자열을 파싱해서 저장")
        void cacheGroupRegion_success() {
            // given
            given(regionRegistry.findGridCoordinate("Seoul", "Jongno")).willReturn(Optional.of(GRID_DTO));

            // when
            GroupRegionDto result = regionService.cacheGroupRegion(GROUP_ID, "Seoul,Jongno");

            // then
            assertThat(result.groupId()).isEqualTo(GROUP_ID);
            assertThat(result.regionGridDto()).isEqualTo(GRID_DTO);

            ArgumentCaptor<GroupRegionDto> captor = ArgumentCaptor.forClass(GroupRegionDto.class);
            verify(groupRegionRepository).save(captor.capture());
            assertThat(captor.getValue().groupId()).isEqualTo(GROUP_ID);
            assertThat(captor.getValue().regionGridDto()).isEqualTo(GRID_DTO);
        }

        @Test
        @DisplayName("groupId 좌표 조회 성공 - 캐시에 있으면 그대로 반환")
        void findByGroupRegion_hit() {
            // given
            GroupRegionDto cached = new GroupRegionDto(GROUP_ID, GRID_DTO, null);
            given(groupRegionRepository.findByGroupId(GROUP_ID)).willReturn(Optional.of(cached));

            // when
            Optional<GroupRegionDto> result = regionService.findByGroupRegion(GROUP_ID);

            // then
            assertThat(result).contains(cached);
        }

        @Test
        @DisplayName("정렬된 시/도 목록 조회 성공")
        void getSortedStates_success() {
            // given
            given(regionRegistry.findAllStates()).willReturn(new java.util.ArrayList<>(List.of("Seoul", "Busan", "Daegu")));

            // when
            List<String> result = regionService.getSortedStates();

            // then
            assertThat(result).containsExactly("Busan", "Daegu", "Seoul");
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

        @Test
        @DisplayName("행정구역 검증 실패 - 존재하지 않는 시/도, 시/군/구 조합")
        void validateRegion_notFound() {
            // given
            given(regionRegistry.findGridCoordinate("Atlantis", "Nowhere")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> regionService.validateRegion("Atlantis", "Nowhere"))
                    .isInstanceOf(RegionNotFoundException.class);
        }

        @Test
        @DisplayName("groupId 좌표 캐싱 실패 - 존재하지 않는 행정구역이면 저장 자체가 안 됨")
        void cacheGroupRegion_invalidRegion() {
            // given
            given(regionRegistry.findGridCoordinate("Atlantis", "Nowhere")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> regionService.cacheGroupRegion(GROUP_ID, "Atlantis,Nowhere"))
                    .isInstanceOf(RegionNotFoundException.class);
            verify(groupRegionRepository, never()).save(any());
        }

        @Test
        @DisplayName("groupId 좌표 캐싱 실패 - 쉼표 없는 레거시(PR 이전) 지역 값이면 AIOOBE 대신 RegionNotFoundException")
        void cacheGroupRegion_legacyFormatWithoutComma() {
            // when & then
            assertThatThrownBy(() -> regionService.cacheGroupRegion(GROUP_ID, "Seoul"))
                    .isInstanceOf(RegionNotFoundException.class);
            verify(groupRegionRepository, never()).save(any());
        }

        @Test
        @DisplayName("groupId 좌표 조회 실패 - 캐시에 없으면 빈 값")
        void findByGroupRegion_miss() {
            // given
            given(groupRegionRepository.findByGroupId(GROUP_ID)).willReturn(Optional.empty());

            // when
            Optional<GroupRegionDto> result = regionService.findByGroupRegion(GROUP_ID);

            // then
            assertThat(result).isEmpty();
        }
    }
}
