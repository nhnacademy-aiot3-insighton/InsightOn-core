package com.insighton.core.usecase.group;

import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.dto.request.GroupUpdateRequest;
import com.insighton.core.domain.groups.event.GroupRegionUpdateEvent;
import com.insighton.core.domain.groups.exception.UnAuthorizedAccessException;
import com.insighton.core.domain.groups.service.GroupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupUpdateUseCaseTest {

    @Mock
    private GroupService groupService;

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GroupUpdateUseCase managementUseCase;
    // ==================== 그룹 수정 ====================

    @Test
    @DisplayName("그룹 수정 성공 - 어드민 권한 확인")
    void updateGroup_success() {
        // given
        GroupUpdateRequest request = new GroupUpdateRequest("name", "desc", null);
        given(groupMemberService.isGroupAdmin(1L, 1L)).willReturn(true);

        // when
        managementUseCase.updateGroup(request, 1L, 1L);

        // then
        verify(groupService, times(1)).updateGroup(request, 1L);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("그룹 수정 성공 - 지역 정보 변경 시 GroupRegionUpdateEvent 이벤트 발행")
    void updateGroup_success_withRegionEvent() {
        // given
        GroupUpdateRequest request = new GroupUpdateRequest("name", "desc", "서울시 강남구");
        given(groupMemberService.isGroupAdmin(1L, 1L)).willReturn(true);

        // when
        managementUseCase.updateGroup(request, 1L, 1L);

        // then
        verify(groupService, times(1)).updateGroup(request, 1L);
        verify(eventPublisher, times(1)).publishEvent(any(GroupRegionUpdateEvent.class));
    }

    @Test
    @DisplayName("그룹 수정 실패 - 어드민 권한 없음")
    void updateGroup_notAdmin() {
        // given
        GroupUpdateRequest request = new GroupUpdateRequest("name", "desc", "loc");
        given(groupMemberService.isGroupAdmin(1L, 1L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> managementUseCase.updateGroup(request, 1L, 1L))
                .isInstanceOf(UnAuthorizedAccessException.class);
    }

}