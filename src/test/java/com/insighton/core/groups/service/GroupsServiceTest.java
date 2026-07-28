package com.insighton.core.groups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.insighton.core.groups.dto.request.GroupsCreateRequest;
import com.insighton.core.groups.dto.request.GroupsUpdateRequest;
import com.insighton.core.groups.dto.response.GroupsListResponse;
import com.insighton.core.groups.dto.response.GroupsResponse;
import com.insighton.core.groups.entity.Groups;
import com.insighton.core.groups.exception.GroupNotFoundException;
import com.insighton.core.groups.exception.InviteTokenNotFoundException;
import com.insighton.core.groups.exception.UnAuthorizedAccessException;
import com.insighton.core.groups.repository.GroupsRepository;
import com.insighton.core.groups.service.impl.GroupsServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GroupsServiceTest {

    @Mock
    private GroupsRepository groupsRepository;

    @InjectMocks
    private GroupsServiceImpl groupsService;

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("그룹 생성 성공")
        void createGroup_success() {
            // given
            GroupsCreateRequest request = new GroupsCreateRequest("Test Group", "Desc", "Loc");
            
            // when
            groupsService.createGroup(request);
            
            // then
            verify(groupsRepository, times(1)).save(any(Groups.class));
        }

        @Test
        @DisplayName("그룹 삭제 성공")
        void deleteGroup_success() {
            // given
            Long groupId = 1L;
            Groups mockGroup = mock(Groups.class);
            given(groupsRepository.findById(groupId)).willReturn(Optional.of(mockGroup));
            
            // when
            groupsService.deleteGroup(groupId);
            
            // then
            verify(groupsRepository, times(1)).delete(mockGroup);
        }

        @Test
        @DisplayName("그룹 정보 수정 성공")
        void updateGroup_success() {
            // given
            Long groupId = 1L;
            GroupsUpdateRequest request = new GroupsUpdateRequest("New Name", "New Desc", "New Loc");
            Groups mockGroup = mock(Groups.class);
            given(groupsRepository.findById(groupId)).willReturn(Optional.of(mockGroup));
            
            // when
            groupsService.updateGroup(request, groupId);
            
            // then
            verify(mockGroup, times(1)).update(request);
        }

        @Test
        @DisplayName("그룹 정보 미리 조회 성공")
        void getGroupPreview_success() {
            // given
            Long groupId = 1L;
            String token = "token";
            Groups mockGroup = Groups.builder().name("T").description("D").location("L").inviteToken(token).build();
            given(groupsRepository.findByInviteTokenAndGroupId(token, groupId)).willReturn(Optional.of(mockGroup));
            
            // when
            GroupsResponse response = groupsService.getGroupPreview(token, groupId);
            
            // then
            assertThat(response.name()).isEqualTo("T");
        }

        @Test
        @DisplayName("관리자 그룹 List 조회 성공")
        void getGroupList_success() {
            // given
            given(groupsRepository.findAll()).willReturn(List.of(Groups.builder().name("T").description("D").location("L").inviteToken("t").build()));
            
            // when
            List<GroupsListResponse> list = groupsService.getGroupList("ADMIN", 1L);
            
            // then
            assertThat(list).hasSize(1);
        }

        @Test
        @DisplayName("새로운 토큰 발급 성공")
        void newInviteToken_success() {
            // given
            Long groupId = 1L;
            Groups mockGroup = mock(Groups.class);
            given(groupsRepository.findById(groupId)).willReturn(Optional.of(mockGroup));
            
            // when
            groupsService.newInviteToken(groupId);
            
            // then
            verify(mockGroup, times(1)).rotateInviteToken(any(String.class));
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

        @Test
        @DisplayName("그룹 삭제 실패 - 존재하지 않는 그룹")
        void deleteGroup_notFound() {
            // given
            Long groupId = 1L;
            given(groupsRepository.findById(groupId)).willReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> groupsService.deleteGroup(groupId))
                    .isInstanceOf(GroupNotFoundException.class);
        }

        @Test
        @DisplayName("그룹 정보 수정 실패 - 존재하지 않는 그룹")
        void updateGroup_notFound() {
            // given
            Long groupId = 1L;
            GroupsUpdateRequest request = new GroupsUpdateRequest("N", "D", "L");
            given(groupsRepository.findById(groupId)).willReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> groupsService.updateGroup(request, groupId))
                    .isInstanceOf(GroupNotFoundException.class);
        }

        @Test
        @DisplayName("그룹 정보 미리 조회 실패 - token이 존재하지 않을 때")
        void getGroupPreview_tokenNotFound() {
            // given
            given(groupsRepository.findByInviteTokenAndGroupId("bad-token", 1L)).willReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> groupsService.getGroupPreview("bad-token", 1L))
                    .isInstanceOf(InviteTokenNotFoundException.class);
        }

        @Test
        @DisplayName("관리자 그룹 List 조회 실패 - 시스템 관리자가 아닐 때")
        void getGroupList_unauthorized() {
            // when & then
            assertThatThrownBy(() -> groupsService.getGroupList("USER", 1L))
                    .isInstanceOf(UnAuthorizedAccessException.class);
        }

        @Test
        @DisplayName("토큰 재발급 실패 - 존재하지 않는 그룹")
        void newInviteToken_notFound() {
            // given
            Long groupId = 1L;
            given(groupsRepository.findById(groupId)).willReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> groupsService.newInviteToken(groupId))
                    .isInstanceOf(GroupNotFoundException.class);
        }
    }
}
