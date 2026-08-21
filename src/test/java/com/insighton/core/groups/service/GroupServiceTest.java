package com.insighton.core.groups.service;

import com.insighton.core.domain.groups.dto.request.GroupRequest;
import com.insighton.core.domain.groups.dto.response.GroupAdminResponse;
import com.insighton.core.domain.groups.dto.response.GroupResponse;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.exception.GroupNotFoundException;
import com.insighton.core.domain.groups.exception.InvitationTokenMismatchException;
import com.insighton.core.domain.groups.exception.InviteTokenNotFoundException;
import com.insighton.core.domain.groups.exception.UnAuthorizedAccessException;
import com.insighton.core.domain.groups.repository.GroupRepository;
import com.insighton.core.domain.groups.service.impl.GroupServiceImpl;
import com.insighton.core.domain.location.exception.EmptyValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private GroupServiceImpl groupService;

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("그룹 생성 성공")
        void createGroup_success() {
            // given
            GroupRequest request = new GroupRequest("Test Group", "Desc", "Loc");

            // when
            groupService.createGroup(request);

            // then
            verify(groupRepository, times(1)).save(any(Group.class));
        }

        @Test
        @DisplayName("그룹 삭제 성공")
        void deleteGroup_success() {
            // given
            Long groupId = 1L;
            Group mockGroup = mock(Group.class);
            given(groupRepository.findById(groupId)).willReturn(Optional.of(mockGroup));

            // when
            groupService.deleteGroup(groupId);

            // then
            verify(groupRepository, times(1)).delete(mockGroup);
        }

        @Test
        @DisplayName("그룹 정보 수정 성공 - 모든 필드 변경 (name, description, groupRegion)")
        void updateGroup_success_allFields() {
            // given
            Long groupId = 1L;
            Group realGroup = Group.builder()
                    .name("Old Name")
                    .description("Old Desc")
                    .groupRegion("Old Loc")
                    .build();
            given(groupRepository.findById(groupId)).willReturn(Optional.of(realGroup));
            GroupRequest request = new GroupRequest("New Name", "New Desc", "New Loc");

            // when
            groupService.updateGroup(request, groupId);

            // then
            assertThat(realGroup.getName()).isEqualTo("New Name");
            assertThat(realGroup.getDescription()).isEqualTo("New Desc");
            assertThat(realGroup.getGroupRegion()).isEqualTo("New Loc");
        }

        @Test
        @DisplayName("그룹 정보 수정 성공 - name만 변경 (description, groupRegion은 null)")
        void updateGroup_success_nameOnly() {
            // given
            Long groupId = 1L;
            Group realGroup = Group.builder()
                    .name("Old Name")
                    .description("Old Desc")
                    .groupRegion("Old Loc")
                    .build();
            given(groupRepository.findById(groupId)).willReturn(Optional.of(realGroup));
            GroupRequest request = new GroupRequest("New Name", null, null);

            // when
            groupService.updateGroup(request, groupId);

            // then
            assertThat(realGroup.getName()).isEqualTo("New Name");
            assertThat(realGroup.getDescription()).isEqualTo("Old Desc");
            assertThat(realGroup.getGroupRegion()).isEqualTo("Old Loc");
        }

        @Test
        @DisplayName("그룹 정보 수정 성공 - description만 변경 (name, groupRegion은 null)")
        void updateGroup_success_descriptionOnly() {
            // given
            Long groupId = 1L;
            Group realGroup = Group.builder()
                    .name("Old Name")
                    .description("Old Desc")
                    .groupRegion("Old Loc")
                    .build();
            given(groupRepository.findById(groupId)).willReturn(Optional.of(realGroup));
            GroupRequest request = new GroupRequest(null, "New Desc", null);

            // when
            groupService.updateGroup(request, groupId);

            // then
            assertThat(realGroup.getName()).isEqualTo("Old Name");
            assertThat(realGroup.getDescription()).isEqualTo("New Desc");
            assertThat(realGroup.getGroupRegion()).isEqualTo("Old Loc");
        }

        @Test
        @DisplayName("그룹 정보 수정 성공 - groupRegion만 변경 (name, description은 null)")
        void updateGroup_success_groupRegionOnly() {
            // given
            Long groupId = 1L;
            Group realGroup = Group.builder()
                    .name("Old Name")
                    .description("Old Desc")
                    .groupRegion("Old Loc")
                    .build();
            given(groupRepository.findById(groupId)).willReturn(Optional.of(realGroup));
            GroupRequest request = new GroupRequest(null, null, "New Loc");

            // when
            groupService.updateGroup(request, groupId);

            // then
            assertThat(realGroup.getName()).isEqualTo("Old Name");
            assertThat(realGroup.getDescription()).isEqualTo("Old Desc");
            assertThat(realGroup.getGroupRegion()).isEqualTo("New Loc");
        }

        @Test
        @DisplayName("그룹 정보 미리 조회 성공")
        void getGroupPreview_success() {
            // given
            Long groupId = 1L;
            String token = "token";
            Group mockGroup = Group.builder().name("T").description("D").groupRegion("L").inviteToken(token).build();
            given(groupRepository.findByInviteTokenAndGroupId(token, groupId)).willReturn(Optional.of(mockGroup));

            // when
            GroupResponse response = groupService.getGroupPreview(token, groupId);

            // then
            assertThat(response.name()).isEqualTo("T");
        }

        @Test
        @DisplayName("관리자 그룹 List 조회 성공")
        void getGroupList_success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            Group group = Group.builder()
                    .name("T")
                    .description("D")
                    .groupRegion("L")
                    .inviteToken("t")
                    .build();

            Page<Group> mockGroupPage = new PageImpl<>(List.of(group));

            given(groupRepository.findAll(pageable)).willReturn(mockGroupPage);

            // when
            Page<GroupAdminResponse> list = groupService.getGroupList("ADMIN", 1L, pageable);

            // then
            assertThat(list).hasSize(1);
            assertThat(list.getContent().getFirst().name()).isEqualTo("T");
        }

        @Test
        @DisplayName("새로운 토큰 발급 성공")
        void newInviteToken_success() {
            // given
            Long groupId = 1L;

            // 실제 객체 생성
            Group realGroup = Group.builder()
                    .name("테스트 그룹")
                    .inviteToken("oldToken1234")
                    .build();

            given(groupRepository.findById(1L)).willReturn(Optional.of(realGroup));

            // when
            groupService.newInviteToken(groupId);

            // then
            // 💡 verify(realGroup) 지우고 assertThat만 남기면 됩니다!
            assertThat(realGroup.getInviteToken()).isNotEqualTo("oldToken1234");
            assertThat(realGroup.getInviteToken()).hasSize(12);
        }

        @Test
        @DisplayName("초대 토큰 검증 성공 - 토큰 일치")
        void validateInviteToken_success() {
            // given
            Long groupId = 1L;
            String token = "validToken12";
            Group group = Group.builder().name("Group").inviteToken(token).build();
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));

            // when & then
            assertDoesNotThrow(() -> groupService.validateInviteToken(groupId, token));
        }

        @Test
        @DisplayName("초대 토큰으로 그룹 조회 성공")
        void validateGroupByInviteToken_success() {
            // given
            String token = "validToken12";
            Group group = Group.builder().name("Group").inviteToken(token).build();
            given(groupRepository.findByInviteToken(token)).willReturn(Optional.of(group));

            // when
            Group result = groupService.validateGroupByInviteToken(token);

            // then
            assertThat(result.getName()).isEqualTo("Group");
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
            given(groupRepository.findById(groupId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> groupService.deleteGroup(groupId))
                    .isInstanceOf(GroupNotFoundException.class);
        }

        @Test
        @DisplayName("그룹 정보 수정 실패 - 존재하지 않는 그룹")
        void updateGroup_notFound() {
            // given
            Long groupId = 1L;
            GroupRequest request = new GroupRequest("N", "D", "L");
            given(groupRepository.findById(groupId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> groupService.updateGroup(request, groupId))
                    .isInstanceOf(GroupNotFoundException.class);
        }

        @Test
        @DisplayName("그룹 정보 미리 조회 실패 - token이 존재하지 않을 때")
        void getGroupPreview_tokenNotFound() {
            // given
            given(groupRepository.findByInviteTokenAndGroupId("bad-token", 1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> groupService.getGroupPreview("bad-token", 1L))
                    .isInstanceOf(InviteTokenNotFoundException.class);
        }

        @Test
        @DisplayName("관리자 그룹 List 조회 실패 - 시스템 관리자가 아닐 때")
        void getGroupList_unauthorized() {
            //given
            Pageable pageable = PageRequest.of(0, 10);

            // when & then
            assertThatThrownBy(() -> groupService.getGroupList("USER", 1L, pageable))
                    .isInstanceOf(UnAuthorizedAccessException.class);
        }

        @Test
        @DisplayName("토큰 재발급 실패 - 존재하지 않는 그룹")
        void newInviteToken_notFound() {
            // given
            Long groupId = 1L;
            given(groupRepository.findById(groupId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> groupService.newInviteToken(groupId))
                    .isInstanceOf(GroupNotFoundException.class);
        }

        @Test
        @DisplayName("토큰 재발급 실패 - 새 토큰이 null이거나 공백이면 EmptyValueException 발생")
        void rotateInviteToken_fail_emptyOrNull() {
            // given
            Group realGroup = Group.builder()
                    .name("테스트 그룹")
                    .inviteToken("oldToken1234")
                    .build();

            // when & then: null 전달 시 예외 발생 검증
            assertThatThrownBy(() -> realGroup.rotateInviteToken(null))
                    .isInstanceOf(EmptyValueException.class);

            // when & then: 공백문자 전달 시 예외 발생 검증
            assertThatThrownBy(() -> realGroup.rotateInviteToken("   "))
                    .isInstanceOf(EmptyValueException.class);
        }

        @Test
        @DisplayName("초대 토큰 검증 실패 - 토큰 불일치 시 InvitationTokenMismatchException 발생")
        void validateInviteToken_mismatch() {
            // given
            Long groupId = 1L;
            Group group = Group.builder().name("Group").inviteToken("correctToken").build();
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));

            // when & then
            assertThatThrownBy(() -> groupService.validateInviteToken(groupId, "wrongToken"))
                    .isInstanceOf(InvitationTokenMismatchException.class);
        }

        @Test
        @DisplayName("초대 토큰으로 그룹 조회 실패 - 존재하지 않는 토큰")
        void validateGroupByInviteToken_notFound() {
            // given
            given(groupRepository.findByInviteToken("invalidToken")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> groupService.validateGroupByInviteToken("invalidToken"))
                    .isInstanceOf(InviteTokenNotFoundException.class);
        }
    }
}
