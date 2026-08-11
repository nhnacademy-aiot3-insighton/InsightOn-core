package com.insighton.core.usecase;

import com.insighton.core.domain.groupregistration.dto.GroupRegistrationResponse;
import com.insighton.core.domain.groupregistration.entity.GroupRegistrationStatus;
import com.insighton.core.domain.groupregistration.exception.AlreadyProcessedException;
import com.insighton.core.domain.groupregistration.exception.GroupRegistrationNotFoundException;
import com.insighton.core.domain.groupregistration.exception.UnauthorizedGroupRegistrationAccessException;
import com.insighton.core.domain.groupregistration.service.GroupRegistrationService;
import com.insighton.core.domain.groups.dto.request.GroupRequest;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.region.service.RegionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GroupRegistrationApprovalUseCaseTest {

    @Mock
    private GroupRegistrationService groupRegistrationService;

    @Mock
    private GroupUseCase groupUseCase;

    @Mock
    private RegionService regionService;

    @InjectMocks
    private GroupRegistrationApprovalUseCase groupRegistrationApprovalUseCase;

    private static final Long GROUP_REGISTRATION_ID = 10L;
    private static final Long APPROVER_ID = 99L;
    private static final Long REQUESTER_ID = 1L;

    private GroupRegistrationResponse approvedResponse() {
        return new GroupRegistrationResponse(GROUP_REGISTRATION_ID, REQUESTER_ID, "Test Group", "Desc", "Seoul",
                GroupRegistrationStatus.APPROVED, APPROVER_ID, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("승인 성공 - 신청 정보 그대로 Group 생성까지 이어짐")
        void approve_success() {
            // given
            Long createdGroupId = 55L;
            Group mockGroup = mock(Group.class);
            given(mockGroup.getGroupId()).willReturn(createdGroupId);
            given(mockGroup.getGroupRegion()).willReturn("Seoul");

            given(groupRegistrationService.approveGroupRegistration("ADMIN", GROUP_REGISTRATION_ID, APPROVER_ID))
                    .willReturn(approvedResponse());
            given(groupUseCase.createGroup(eq(new GroupRequest("Test Group", "Desc", "Seoul")), eq(REQUESTER_ID)))
                    .willReturn(mockGroup);

            // when
            groupRegistrationApprovalUseCase.approve("ADMIN", GROUP_REGISTRATION_ID, APPROVER_ID);

            // then
            verify(groupUseCase).createGroup(
                    eq(new GroupRequest("Test Group", "Desc", "Seoul")),
                    eq(REQUESTER_ID));
            verify(regionService).cacheGroupRegion(createdGroupId, "Seoul");
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

        @Test
        @DisplayName("승인 실패 - 관리자가 아니면 Group 생성으로 이어지지 않음")
        void approve_unauthorized() {
            // given
            willThrow(new UnauthorizedGroupRegistrationAccessException())
                    .given(groupRegistrationService).approveGroupRegistration("USER", GROUP_REGISTRATION_ID, APPROVER_ID);

            // when & then
            assertThatThrownBy(() -> groupRegistrationApprovalUseCase.approve("USER", GROUP_REGISTRATION_ID, APPROVER_ID))
                    .isInstanceOf(UnauthorizedGroupRegistrationAccessException.class);
            verify(groupUseCase, never()).createGroup(any(), any());
        }

        @Test
        @DisplayName("승인 실패 - 존재하지 않는 신청이면 Group 생성으로 이어지지 않음")
        void approve_notFound() {
            // given
            willThrow(new GroupRegistrationNotFoundException())
                    .given(groupRegistrationService).approveGroupRegistration("ADMIN", GROUP_REGISTRATION_ID, APPROVER_ID);

            // when & then
            assertThatThrownBy(() -> groupRegistrationApprovalUseCase.approve("ADMIN", GROUP_REGISTRATION_ID, APPROVER_ID))
                    .isInstanceOf(GroupRegistrationNotFoundException.class);
            verify(groupUseCase, never()).createGroup(any(), any());
        }

        @Test
        @DisplayName("승인 실패 - 동시 처리로 인한 낙관적 락 충돌 시 Group 생성으로 이어지지 않음")
        void approve_optimisticLockConflict() {
            // given
            willThrow(AlreadyProcessedException.of())
                    .given(groupRegistrationService).approveGroupRegistration("ADMIN", GROUP_REGISTRATION_ID, APPROVER_ID);

            // when & then
            assertThatThrownBy(() -> groupRegistrationApprovalUseCase.approve("ADMIN", GROUP_REGISTRATION_ID, APPROVER_ID))
                    .isInstanceOf(AlreadyProcessedException.class);
            verify(groupUseCase, never()).createGroup(any(), any());
        }
    }
}
