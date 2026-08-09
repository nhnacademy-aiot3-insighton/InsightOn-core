package com.insighton.core.groupregistration.service;

import com.insighton.core.domain.groupregistration.presentation.CreateGroupRegistrationRequest;
import com.insighton.core.domain.groupregistration.presentation.GroupRegistrationResponse;
import com.insighton.core.domain.groupregistration.entity.GroupRegistration;
import com.insighton.core.domain.groupregistration.entity.GroupRegistrationStatus;
import com.insighton.core.domain.groupregistration.exception.AlreadyProcessedException;
import com.insighton.core.domain.groupregistration.exception.AlreadyRequestedException;
import com.insighton.core.domain.groupregistration.exception.GroupRegistrationNotFoundException;
import com.insighton.core.domain.groupregistration.exception.UnauthorizedGroupRegistrationAccessException;
import com.insighton.core.domain.groupregistration.mapper.GroupRegistrationMapper;
import com.insighton.core.domain.groupregistration.repository.GroupRegistrationRepository;
import com.insighton.core.domain.groupregistration.service.GroupRegistrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupRegistrationServiceTest {

    @Mock
    private GroupRegistrationRepository groupRegistrationRepository;

    @Mock
    private GroupRegistrationMapper groupRegistrationMapper;

    @InjectMocks
    private GroupRegistrationService groupRegistrationService;

    private static final Long REQUESTER_ID = 1L;
    private static final Long GROUP_REGISTRATION_ID = 10L;

    private GroupRegistration pendingRegistration() {
        return GroupRegistration.create(REQUESTER_ID, "Test Group", "Desc", "Seoul");
    }

    private GroupRegistrationResponse dummyResponse(GroupRegistrationStatus status) {
        return new GroupRegistrationResponse(GROUP_REGISTRATION_ID, REQUESTER_ID, "Test Group", "Desc", "Seoul",
                status, null, OffsetDateTime.now(), null);
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("그룹 등록 신청 생성 성공")
        void createRequest_success() {
            // given
            CreateGroupRegistrationRequest request = new CreateGroupRegistrationRequest("Test Group", "Desc", "Seoul");
            GroupRegistration saved = pendingRegistration();
            GroupRegistrationResponse response = dummyResponse(GroupRegistrationStatus.PENDING);

            given(groupRegistrationRepository.existsByRequesterIdAndStatus(REQUESTER_ID, GroupRegistrationStatus.PENDING))
                    .willReturn(false);
            given(groupRegistrationRepository.save(any(GroupRegistration.class))).willReturn(saved);
            given(groupRegistrationMapper.toResponse(saved)).willReturn(response);

            // when
            GroupRegistrationResponse result = groupRegistrationService.createRequest(REQUESTER_ID, request);

            // then
            assertThat(result).isEqualTo(response);
            verify(groupRegistrationRepository).flush();
        }

        @Test
        @DisplayName("관리자 - 전체 신청 목록 조회 성공")
        void getGroupRegistrations_success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<GroupRegistration> page = new PageImpl<>(List.of(pendingRegistration()));
            given(groupRegistrationRepository.findAll(pageable)).willReturn(page);
            given(groupRegistrationMapper.toResponse(any(GroupRegistration.class)))
                    .willReturn(dummyResponse(GroupRegistrationStatus.PENDING));

            // when
            Page<GroupRegistrationResponse> result = groupRegistrationService.getGroupRegistrations("ADMIN", null, pageable);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("내 신청 목록 조회 성공")
        void getMyGroupRegistrations_success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<GroupRegistration> page = new PageImpl<>(List.of(pendingRegistration()));
            given(groupRegistrationRepository.findAllByRequesterId(REQUESTER_ID, pageable)).willReturn(page);
            given(groupRegistrationMapper.toResponse(any(GroupRegistration.class)))
                    .willReturn(dummyResponse(GroupRegistrationStatus.PENDING));

            // when
            Page<GroupRegistrationResponse> result = groupRegistrationService.getMyGroupRegistrations(REQUESTER_ID, pageable);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("신청 취소 성공")
        void cancelGroupRegistration_success() {
            // given
            GroupRegistration registration = pendingRegistration();
            given(groupRegistrationRepository.findById(GROUP_REGISTRATION_ID)).willReturn(Optional.of(registration));

            // when
            groupRegistrationService.cancelGroupRegistration(GROUP_REGISTRATION_ID, REQUESTER_ID);

            // then
            assertThat(registration.getStatus()).isEqualTo(GroupRegistrationStatus.CANCELLED);
            verify(groupRegistrationRepository).flush();
        }

        @Test
        @DisplayName("신청 승인 성공")
        void approveGroupRegistration_success() {
            // given
            Long approverId = 99L;
            GroupRegistration registration = pendingRegistration();
            GroupRegistrationResponse response = dummyResponse(GroupRegistrationStatus.APPROVED);

            given(groupRegistrationRepository.findById(GROUP_REGISTRATION_ID)).willReturn(Optional.of(registration));
            given(groupRegistrationMapper.toResponse(registration)).willReturn(response);

            // when
            GroupRegistrationResponse result = groupRegistrationService.approveGroupRegistration("ADMIN", GROUP_REGISTRATION_ID, approverId);

            // then
            assertThat(result).isEqualTo(response);
            assertThat(registration.getStatus()).isEqualTo(GroupRegistrationStatus.APPROVED);
            verify(groupRegistrationRepository).flush();
        }

        @Test
        @DisplayName("신청 거절 성공")
        void rejectGroupRegistration_success() {
            // given
            Long approverId = 99L;
            GroupRegistration registration = pendingRegistration();
            given(groupRegistrationRepository.findById(GROUP_REGISTRATION_ID)).willReturn(Optional.of(registration));

            // when
            groupRegistrationService.rejectGroupRegistration("ADMIN", GROUP_REGISTRATION_ID, approverId);

            // then
            assertThat(registration.getStatus()).isEqualTo(GroupRegistrationStatus.REJECTED);
            verify(groupRegistrationRepository).flush();
        }

        @Test
        @DisplayName("신청 상세 조회 성공 - 본인")
        void getGroupRegistration_success_owner() {
            // given
            GroupRegistration registration = pendingRegistration();
            GroupRegistrationResponse response = dummyResponse(GroupRegistrationStatus.PENDING);
            given(groupRegistrationRepository.findById(GROUP_REGISTRATION_ID)).willReturn(Optional.of(registration));
            given(groupRegistrationMapper.toResponse(registration)).willReturn(response);

            // when
            GroupRegistrationResponse result = groupRegistrationService.getGroupRegistration(GROUP_REGISTRATION_ID, REQUESTER_ID, null);

            // then
            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("신청 상세 조회 성공 - 관리자")
        void getGroupRegistration_success_admin() {
            // given
            Long otherUserId = 2L;
            GroupRegistration registration = pendingRegistration();
            GroupRegistrationResponse response = dummyResponse(GroupRegistrationStatus.PENDING);
            given(groupRegistrationRepository.findById(GROUP_REGISTRATION_ID)).willReturn(Optional.of(registration));
            given(groupRegistrationMapper.toResponse(registration)).willReturn(response);

            // when
            GroupRegistrationResponse result = groupRegistrationService.getGroupRegistration(GROUP_REGISTRATION_ID, otherUserId, "ADMIN");

            // then
            assertThat(result).isEqualTo(response);
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

        @Test
        @DisplayName("신청 생성 실패 - 이미 대기중인 신청 존재")
        void createRequest_alreadyPending() {
            // given
            CreateGroupRegistrationRequest request = new CreateGroupRegistrationRequest("Test Group", "Desc", "Seoul");
            given(groupRegistrationRepository.existsByRequesterIdAndStatus(REQUESTER_ID, GroupRegistrationStatus.PENDING))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> groupRegistrationService.createRequest(REQUESTER_ID, request))
                    .isInstanceOf(AlreadyRequestedException.class);
            verify(groupRegistrationRepository, never()).save(any());
        }

        @Test
        @DisplayName("신청 생성 실패 - 동시 요청으로 인한 DB 제약 위반")
        void createRequest_raceCondition() {
            // given
            CreateGroupRegistrationRequest request = new CreateGroupRegistrationRequest("Test Group", "Desc", "Seoul");
            given(groupRegistrationRepository.existsByRequesterIdAndStatus(REQUESTER_ID, GroupRegistrationStatus.PENDING))
                    .willReturn(false);
            given(groupRegistrationRepository.save(any(GroupRegistration.class))).willReturn(pendingRegistration());
            doThrow(new DataIntegrityViolationException("duplicate"))
                    .when(groupRegistrationRepository).flush();

            // when & then
            assertThatThrownBy(() -> groupRegistrationService.createRequest(REQUESTER_ID, request))
                    .isInstanceOf(AlreadyRequestedException.class);
        }

        @Test
        @DisplayName("관리자 - 전체 신청 목록 조회 실패 - 관리자가 아님")
        void getGroupRegistrations_unauthorized() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when & then
            assertThatThrownBy(() -> groupRegistrationService.getGroupRegistrations("USER", null, pageable))
                    .isInstanceOf(UnauthorizedGroupRegistrationAccessException.class);
            verify(groupRegistrationRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("신청 취소 실패 - 존재하지 않는 신청")
        void cancelGroupRegistration_notFound() {
            // given
            given(groupRegistrationRepository.findById(GROUP_REGISTRATION_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> groupRegistrationService.cancelGroupRegistration(GROUP_REGISTRATION_ID, REQUESTER_ID))
                    .isInstanceOf(GroupRegistrationNotFoundException.class);
        }

        @Test
        @DisplayName("신청 취소 실패 - 본인의 신청이 아님")
        void cancelGroupRegistration_notOwner() {
            // given
            Long otherUserId = 2L;
            GroupRegistration registration = pendingRegistration();
            given(groupRegistrationRepository.findById(GROUP_REGISTRATION_ID)).willReturn(Optional.of(registration));

            // when & then
            assertThatThrownBy(() -> groupRegistrationService.cancelGroupRegistration(GROUP_REGISTRATION_ID, otherUserId))
                    .isInstanceOf(UnauthorizedGroupRegistrationAccessException.class);
        }

        @Test
        @DisplayName("신청 취소 실패 - 이미 처리된 신청")
        void cancelGroupRegistration_alreadyProcessed() {
            // given
            GroupRegistration registration = pendingRegistration();
            registration.approve(99L);
            given(groupRegistrationRepository.findById(GROUP_REGISTRATION_ID)).willReturn(Optional.of(registration));

            // when & then
            assertThatThrownBy(() -> groupRegistrationService.cancelGroupRegistration(GROUP_REGISTRATION_ID, REQUESTER_ID))
                    .isInstanceOf(AlreadyProcessedException.class);
        }

        @Test
        @DisplayName("신청 승인 실패 - 관리자가 아님")
        void approveGroupRegistration_unauthorized() {
            // when & then
            assertThatThrownBy(() -> groupRegistrationService.approveGroupRegistration("USER", GROUP_REGISTRATION_ID, 99L))
                    .isInstanceOf(UnauthorizedGroupRegistrationAccessException.class);
            verify(groupRegistrationRepository, never()).findById(any());
        }

        @Test
        @DisplayName("신청 승인 실패 - 존재하지 않는 신청")
        void approveGroupRegistration_notFound() {
            // given
            given(groupRegistrationRepository.findById(GROUP_REGISTRATION_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> groupRegistrationService.approveGroupRegistration("ADMIN", GROUP_REGISTRATION_ID, 99L))
                    .isInstanceOf(GroupRegistrationNotFoundException.class);
        }

        @Test
        @DisplayName("신청 승인 실패 - 동시 승인으로 인한 낙관적 락 충돌")
        void approveGroupRegistration_optimisticLockConflict() {
            // given
            GroupRegistration registration = pendingRegistration();
            given(groupRegistrationRepository.findById(GROUP_REGISTRATION_ID)).willReturn(Optional.of(registration));
            doThrow(new ObjectOptimisticLockingFailureException(GroupRegistration.class, GROUP_REGISTRATION_ID))
                    .when(groupRegistrationRepository).flush();

            // when & then
            assertThatThrownBy(() -> groupRegistrationService.approveGroupRegistration("ADMIN", GROUP_REGISTRATION_ID, 99L))
                    .isInstanceOf(AlreadyProcessedException.class);
        }

        @Test
        @DisplayName("신청 거절 실패 - 관리자가 아님")
        void rejectGroupRegistration_unauthorized() {
            // when & then
            assertThatThrownBy(() -> groupRegistrationService.rejectGroupRegistration("USER", GROUP_REGISTRATION_ID, 99L))
                    .isInstanceOf(UnauthorizedGroupRegistrationAccessException.class);
            verify(groupRegistrationRepository, never()).findById(any());
        }

        @Test
        @DisplayName("신청 거절 실패 - 동시 처리로 인한 낙관적 락 충돌")
        void rejectGroupRegistration_optimisticLockConflict() {
            // given
            GroupRegistration registration = pendingRegistration();
            given(groupRegistrationRepository.findById(GROUP_REGISTRATION_ID)).willReturn(Optional.of(registration));
            doThrow(new ObjectOptimisticLockingFailureException(GroupRegistration.class, GROUP_REGISTRATION_ID))
                    .when(groupRegistrationRepository).flush();

            // when & then
            assertThatThrownBy(() -> groupRegistrationService.rejectGroupRegistration("ADMIN", GROUP_REGISTRATION_ID, 99L))
                    .isInstanceOf(AlreadyProcessedException.class);
        }

        @Test
        @DisplayName("신청 상세 조회 실패 - 본인도 관리자도 아님")
        void getGroupRegistration_unauthorized() {
            // given
            Long otherUserId = 2L;
            GroupRegistration registration = pendingRegistration();
            given(groupRegistrationRepository.findById(GROUP_REGISTRATION_ID)).willReturn(Optional.of(registration));

            // when & then
            assertThatThrownBy(() -> groupRegistrationService.getGroupRegistration(GROUP_REGISTRATION_ID, otherUserId, "USER"))
                    .isInstanceOf(UnauthorizedGroupRegistrationAccessException.class);
        }

        @Test
        @DisplayName("신청 상세 조회 실패 - 존재하지 않는 신청")
        void getGroupRegistration_notFound() {
            // given
            given(groupRegistrationRepository.findById(GROUP_REGISTRATION_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> groupRegistrationService.getGroupRegistration(GROUP_REGISTRATION_ID, REQUESTER_ID, null))
                    .isInstanceOf(GroupRegistrationNotFoundException.class);
        }
    }
}
