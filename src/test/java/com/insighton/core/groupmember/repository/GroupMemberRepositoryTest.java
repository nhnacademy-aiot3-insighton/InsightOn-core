package com.insighton.core.groupmember.repository;

import com.insighton.core.groupmember.dto.response.GroupMemberListResponse;
import com.insighton.core.groupmember.entity.GroupMember;
import com.insighton.core.groups.entity.Group;
import com.insighton.core.groups.repository.GroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/group-members-test.sql")
class GroupMemberRepositoryTest {

    @Autowired
    GroupMemberRepository groupMemberRepository;

    @Autowired
    GroupRepository groupRepository;

    @Test
    @DisplayName("사용자 ID로 그룹 멤버 조회")
    void findByUserId_success() {
        // given
        Group group = groupRepository.findById(1L).orElseThrow();
        GroupMember member = GroupMember.builder()
                .group(group)
                .userId(100L)
                .groupRole(GroupMember.GroupRole.MEMBER)
                .build();
        groupMemberRepository.save(member);

        // when
        Optional<GroupMember> found = groupMemberRepository.findByUserId(100L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("그룹 ID와 사용자 ID로 그룹 멤버 조회")
    void findByGroup_GroupIdAndUserId_success() {
        // given
        Group group = groupRepository.findById(1L).orElseThrow();
        GroupMember member = GroupMember.builder()
                .group(group)
                .userId(200L)
                .groupRole(GroupMember.GroupRole.MEMBER)
                .build();
        groupMemberRepository.save(member);

        // when
        Optional<GroupMember> found = groupMemberRepository.findByGroupGroupIdAndUserId(group.getGroupId(), 200L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("이미 존재하는 멤버인지 확인")
    void existsByGroup_GroupIdAndUserId_success() {
        // given
        Group group = groupRepository.findById(1L).orElseThrow();
        GroupMember member = GroupMember.builder()
                .group(group)
                .userId(300L)
                .groupRole(GroupMember.GroupRole.MEMBER)
                .build();
        groupMemberRepository.save(member);

        // when
        boolean exists = groupMemberRepository.existsByGroupGroupIdAndUserId(group.getGroupId(), 300L);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("사용자 ID 존재 여부 확인")
    void existsByUserId() {
        assertThat(groupMemberRepository.existsByUserId(1L)).isTrue();
    }

    @Test
    @DisplayName("사용자 ID로 그룹 멤버 조회")
    void findByUserId() {
        Optional<GroupMember> found = groupMemberRepository.findByUserId(1L);
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("그룹 ID로 멤버 목록 조회 (DTO 변환 확인)")
    void findAllByGroupGroupId() {
        List<GroupMemberListResponse> list = groupMemberRepository.findAllByGroupGroupId(1L);
        assertThat(list).hasSize(1);
    }

    @Test
    @DisplayName("그룹 ID와 사용자 ID로 존재 여부 확인")
    void existsByGroupGroupIdAndUserId() {
        assertThat(groupMemberRepository.existsByGroupGroupIdAndUserId(1L, 1L)).isTrue();
    }

    @Test
    @DisplayName("그룹 ID와 사용자 ID로 조회")
    void findByGroupGroupIdAndUserId() {
        Optional<GroupMember> found = groupMemberRepository.findByGroupGroupIdAndUserId(1L, 1L);
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("멤버 ID와 그룹 ID로 조회")
    void findByGroupMemberIdAndGroupGroupId() {
        // SQL에서 삽입된 데이터의 groupMemberId가 1이라고 가정
        Optional<GroupMember> found = groupMemberRepository.findByGroupMemberIdAndGroupGroupId(1L, 1L);
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("저장 테스트")
    void save() {
        Group group = groupRepository.findById(1L).orElseThrow();
        GroupMember member = GroupMember.builder()
                .group(group)
                .userId(99L)
                .groupRole(GroupMember.GroupRole.MEMBER)
                .build();

        GroupMember saved = groupMemberRepository.save(member);
        assertThat(saved.getGroupMemberId()).isNotNull();
    }

    @Test
    @DisplayName("그룹 ID로 모든 멤버 삭제")
    void deleteAllByGroupGroupId() {
        groupMemberRepository.deleteAllByGroupGroupId(1L);

        // 삭제 후 확인
        assertThat(groupMemberRepository.findAllByGroupGroupId(1L)).isEmpty();
    }

}
