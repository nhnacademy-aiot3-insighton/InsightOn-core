package com.insighton.core.usecase;

import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.service.GroupService;
import com.insighton.core.domain.region.dto.GroupRegionDto;
import com.insighton.core.domain.region.dto.RegionGridDto;
import com.insighton.core.domain.region.service.RegionService;
import com.insighton.core.usecase.groupregistration.GroupRegionResolutionUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupRegionResolutionUseCaseTest {

    private static final Long GROUP_ID = 1L;
    private static final RegionGridDto GRID_DTO = new RegionGridDto("Seoul", "Jongno", 60, 127);
    @Mock
    private RegionService regionService;
    @Mock
    private GroupService groupService;
    @InjectMocks
    private GroupRegionResolutionUseCase groupRegionResolutionUseCase;

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("캐시 히트 - Group을 다시 조회하지 않고 캐시된 값을 그대로 반환")
        void resolve_cacheHit() {
            // given
            GroupRegionDto cached = new GroupRegionDto(GROUP_ID, GRID_DTO, null);
            given(regionService.findByGroupRegion(GROUP_ID)).willReturn(Optional.of(cached));

            // when
            GroupRegionDto result = groupRegionResolutionUseCase.resolve(GROUP_ID);

            // then
            assertThat(result).isEqualTo(cached);
            verify(groupService, never()).groupFindById(any());
            verify(regionService, never()).cacheGroupRegion(any(), any());
        }

        @Test
        @DisplayName("캐시 미스 - Group을 다시 조회해서 groupRegion으로 캐시를 복구")
        void resolve_cacheMiss_rebuildsFromGroup() {
            // given
            Group mockGroup = mock(Group.class);
            given(mockGroup.getGroupRegion()).willReturn("Seoul,Jongno");
            GroupRegionDto rebuilt = new GroupRegionDto(GROUP_ID, GRID_DTO, null);

            given(regionService.findByGroupRegion(GROUP_ID)).willReturn(Optional.empty());
            given(groupService.groupFindById(GROUP_ID)).willReturn(mockGroup);
            given(regionService.cacheGroupRegion(GROUP_ID, "Seoul,Jongno")).willReturn(rebuilt);

            // when
            GroupRegionDto result = groupRegionResolutionUseCase.resolve(GROUP_ID);

            // then
            assertThat(result).isEqualTo(rebuilt);
            verify(groupService).groupFindById(GROUP_ID);
            verify(regionService).cacheGroupRegion(GROUP_ID, "Seoul,Jongno");
        }
    }
}
