package com.insighton.core.groups.repository;

import com.insighton.core.groups.entity.Group;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test") // 1. test 프로파일(application-test.properties)을 읽어오도록 지정
@Sql(scripts = "/group-test.sql")
class GroupRepositoryTest {

    @Autowired
    GroupRepository groupRepository;

    @Test
    @DisplayName("초대 토큰으로 그룹 조회 성공")
    void findByInviteToken_success() {
        // given
        // SQL에서 기본 데이터가 삽입되므로 별도 저장 안 함

        // when
        Optional<Group> found = groupRepository.findByInviteToken("test-token");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("test-group");
    }

    @Test
    @DisplayName("초대 토큰과 그룹 ID로 그룹 조회 성공")
    void findByInviteTokenAndGroupId_success() {
        // given
        // 1L은 SQL에서 생성된 기본 그룹의 ID

        // when
        Optional<Group> found = groupRepository.findByInviteTokenAndGroupId("test-token", 1L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getGroupId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("그룹 저장 및 ID로 조회 성공")
    void saveAndFindById_success() {
        // given
        Group group = Group.builder()
                .name("new-group")
                .groupRegion("Seoul")
                .inviteToken("new-token")
                .build();
        group = groupRepository.save(group);

        // when
        Optional<Group> found = groupRepository.findById(group.getGroupId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("new-group");
    }

    @Test
    @DisplayName("그룹 삭제 성공")
    void delete_success() {
        // given
        Long groupId = 1L;

        // when
        groupRepository.deleteById(groupId);

        // then
        assertThat(groupRepository.findById(groupId)).isEmpty();
    }
}