package com.insighton.core.groupmember.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.insighton.core.groupmember.dto.response.GroupMembersListResponse;
import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groups.entity.Groups;
import com.insighton.core.groups.repository.GroupsRepository;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/group-members-test.sql")
class GroupMembersRepositoryTest {

    @Autowired
    GroupMembersRepository groupMembersRepository;

    @Autowired
    GroupsRepository groupsRepository;

    @Test
    @DisplayName("사용자 ID로 그룹 멤버 조회")
    void findByUserId_success() {
        // given
        Groups group = groupsRepository.findById(1L).orElseThrow();
        GroupMembers member = GroupMembers.builder()
                .groups(group)
                .userId(100L)
                .groupRole(GroupMembers.GroupRole.MEMBER)
                .build();
        groupMembersRepository.save(member);

        // when
        Optional<GroupMembers> found = groupMembersRepository.findByUserId(100L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("그룹 ID와 사용자 ID로 그룹 멤버 조회")
    void findByGroups_GroupIdAndUserId_success() {
        // given
        Groups group = groupsRepository.findById(1L).orElseThrow();
        GroupMembers member = GroupMembers.builder()
                .groups(group)
                .userId(200L)
                .groupRole(GroupMembers.GroupRole.MEMBER)
                .build();
        groupMembersRepository.save(member);

        // when
        Optional<GroupMembers> found = groupMembersRepository.findByGroups_GroupIdAndUserId(group.getGroupId(), 200L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("이미 존재하는 멤버인지 확인")
    void existsByGroups_GroupIdAndUserId_success() {
        // given
        Groups group = groupsRepository.findById(1L).orElseThrow();
        GroupMembers member = GroupMembers.builder()
                .groups(group)
                .userId(300L)
                .groupRole(GroupMembers.GroupRole.MEMBER)
                .build();
        groupMembersRepository.save(member);

        // when
        boolean exists = groupMembersRepository.existsByGroups_GroupIdAndUserId(group.getGroupId(), 300L);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("사용자 ID 존재 여부 확인")
    void existsByUserId() {
        assertThat(groupMembersRepository.existsByUserId(1L)).isTrue();
    }

    @Test
    @DisplayName("사용자 ID로 그룹 멤버 조회")
    void findByUserId() {
        Optional<GroupMembers> found = groupMembersRepository.findByUserId(1L);
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("그룹 ID로 멤버 목록 조회 (DTO 변환 확인)")
    void findAllByGroups_GroupId() {
        List<GroupMembersListResponse> list = groupMembersRepository.findAllByGroups_GroupId(1L);
        assertThat(list).hasSize(1);
    }

    @Test
    @DisplayName("그룹 ID와 사용자 ID로 존재 여부 확인")
    void existsByGroups_GroupIdAndUserId() {
        assertThat(groupMembersRepository.existsByGroups_GroupIdAndUserId(1L, 1L)).isTrue();
    }

    @Test
    @DisplayName("그룹 ID와 사용자 ID로 조회")
    void findByGroups_GroupIdAndUserId() {
        Optional<GroupMembers> found = groupMembersRepository.findByGroups_GroupIdAndUserId(1L, 1L);
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("멤버 ID와 그룹 ID로 조회")
    void findByGroupMemberIdAndGroups_GroupId() {
        // SQL에서 삽입된 데이터의 groupMemberId가 1이라고 가정
        Optional<GroupMembers> found = groupMembersRepository.findByGroupMemberIdAndGroups_GroupId(1L, 1L);
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("저장 테스트")
    void save() {
        Groups group = groupsRepository.findById(1L).orElseThrow();
        GroupMembers member = GroupMembers.builder()
                .groups(group)
                .userId(99L)
                .groupRole(GroupMembers.GroupRole.MEMBER)
                .build();

        GroupMembers saved = groupMembersRepository.save(member);
        assertThat(saved.getGroupMemberId()).isNotNull();
    }

    @Test
    @DisplayName("그룹 ID로 모든 멤버 삭제")
    void deleteAllByGroups_GroupId() {
        groupMembersRepository.deleteAllByGroups_GroupId(1L);

        // 삭제 후 확인
        assertThat(groupMembersRepository.findAllByGroups_GroupId(1L)).isEmpty();
    }

}
