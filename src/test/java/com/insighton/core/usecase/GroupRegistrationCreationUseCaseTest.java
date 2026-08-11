package com.insighton.core.usecase;

import com.insighton.core.domain.groupregistration.dto.CreateGroupRegistrationRequest;
import com.insighton.core.domain.groupregistration.dto.GroupRegistrationResponse;
import com.insighton.core.domain.groupregistration.entity.GroupRegistrationStatus;
import com.insighton.core.domain.groupregistration.service.GroupRegistrationService;
import com.insighton.core.domain.region.dto.RegionGridDto;
import com.insighton.core.domain.region.exception.RegionNotFoundException;
import com.insighton.core.domain.region.service.RegionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GroupRegistrationCreationUseCaseTest {

    @Mock
    private GroupRegistrationService groupRegistrationService;

    @Mock
    private RegionService regionService;

    @InjectMocks
    private GroupRegistrationCreationUseCase groupRegistrationCreationUseCase;

    private static final Long REQUESTER_ID = 1L;

    private GroupRegistrationResponse dummyResponse() {
        return new GroupRegistrationResponse(10L, REQUESTER_ID, "Test Group", "Desc", "Seoul,Jongno",
                GroupRegistrationStatus.PENDING, null, OffsetDateTime.now(), null);
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("신청 생성 성공 - 지역 검증 후 state,city로 조합해서 Service에 위임")
        void createRequest_success() {
            // given
            CreateGroupRegistrationRequest request = new CreateGroupRegistrationRequest("Test Group", "Desc", "Seoul", "Jongno");
            GroupRegistrationResponse expected = dummyResponse();
            given(regionService.validateRegion("Seoul", "Jongno"))
                    .willReturn(new RegionGridDto("Seoul", "Jongno", 60, 127));
            given(groupRegistrationService.createRequest(REQUESTER_ID, "Test Group", "Desc", "Seoul,Jongno"))
                    .willReturn(expected);

            // when
            GroupRegistrationResponse result = groupRegistrationCreationUseCase.createRequest(REQUESTER_ID, request);

            // then
            assertThat(result).isEqualTo(expected);
            verify(groupRegistrationService).createRequest(REQUESTER_ID, "Test Group", "Desc", "Seoul,Jongno");
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

        @Test
        @DisplayName("신청 생성 실패 - 존재하지 않는 행정구역이면 신청 저장 자체가 안 됨")
        void createRequest_invalidRegion() {
            // given
            CreateGroupRegistrationRequest request = new CreateGroupRegistrationRequest("Test Group", "Desc", "Atlantis", "Nowhere");
            willThrow(new RegionNotFoundException("존재하지 않는 행정구역입니다: Atlantis Nowhere"))
                    .given(regionService).validateRegion("Atlantis", "Nowhere");

            // when & then
            assertThatThrownBy(() -> groupRegistrationCreationUseCase.createRequest(REQUESTER_ID, request))
                    .isInstanceOf(RegionNotFoundException.class);
            verify(groupRegistrationService, never()).createRequest(any(), any(), any(), any());
        }
    }
}
