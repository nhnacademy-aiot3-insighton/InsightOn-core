package com.insighton.core.groups.repository;

import com.insighton.core.groups.entity.Groups;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;


import java.util.Optional;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test") // 1. test 프로파일(application-test.properties)을 읽어오도록 지정
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/group-test.sql")
class GroupsRepositoryTest {

    @Autowired
    GroupsRepository groupsRepository;

    @Test
    @DisplayName("초대 토큰으로 그룹 조회 성공")
    void findByInviteToken_success() {
        // given
        // SQL에서 기본 데이터가 삽입되므로 별도 저장 안 함

        // when
        Optional<Groups> found = groupsRepository.findByInviteToken("test-token");

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
        Optional<Groups> found = groupsRepository.findByInviteTokenAndGroupId("test-token", 1L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getGroupId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("그룹 저장 및 ID로 조회 성공")
    void saveAndFindById_success() {
        // given
        Groups group = Groups.builder()
                .name("new-group")
                .location("Seoul")
                .inviteToken("new-token")
                .build();
        group = groupsRepository.save(group);

        // when
        Optional<Groups> found = groupsRepository.findById(group.getGroupId());

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
        groupsRepository.deleteById(groupId);

        // then
        assertThat(groupsRepository.findById(groupId)).isEmpty();
    }
}