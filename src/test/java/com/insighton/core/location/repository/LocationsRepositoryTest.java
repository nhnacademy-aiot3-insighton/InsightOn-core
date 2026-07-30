package com.insighton.core.location.repository;

import com.insighton.core.location.dto.response.LocationsListResponse;
import com.insighton.core.location.entity.Locations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@ActiveProfiles("test") // 1. test 프로파일(application-test.properties)을 읽어오도록 지정
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/location-test.sql")
class LocationsRepositoryTest {

    @Autowired
    LocationsRepository locationsRepository;

    @Nested
    @DisplayName("성공")
    class success {
        @Test
        @DisplayName("그룹의 아이디로 해당하는 모든 location 조회 성공")
        void findAllByGroups_GroupId_success() {
            Long groupId = 1L;

            List<LocationsListResponse> found = locationsRepository.findAllByGroups_GroupId(groupId);

            assertThat(found).hasSize(1);
            assertThat(found.getFirst().locationName()).isEqualTo("test-name");
        }

        @Test
        @DisplayName("그룹의 아이디와  location 아이디 둘 다 해당하는 location의 상세 정보 조회 성공")
        void findByLocationIdAndGroups_GroupId_success() {
            Long groupId = 1L;
            Long locationId = 1L;

            Optional<Locations> found = locationsRepository.findByLocationIdAndGroups_GroupId(locationId, groupId);

            assertThat(found).isPresent();
            assertThat(found.get().getLocationName()).isEqualTo("test-name");
        }

        @Test
        @DisplayName("그룹의 아이디와 location 이름 둘 다 해당하는 Location이 있는지 확인 성공")
        void existsByGroups_GroupIdAndLocationName_success() {
            Long groupId = 1L;
            String locationName = "test-name";

            boolean success = locationsRepository.existsByGroups_GroupIdAndLocationName(groupId, locationName);

            assertThat(success).isTrue();
        }

        @Test
        @DisplayName("그룹 ID에 해당하는 모든 location 삭제 성공")
        void deleteByGroups_GroupId_success() {
            // given
            Long groupId = 1L;

            // when
            locationsRepository.deleteAllByGroups_GroupId(groupId);

            // then
            List<LocationsListResponse> found = locationsRepository.findAllByGroups_GroupId(groupId);
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("실패")
    class Failure {
        @Test
        @DisplayName("존재하지 않는 그룹 ID로 location 조회 시 빈 리스트 반환")
        void findAllByGroups_GroupId_empty() {
            // given
            Long failGroupId = 999L;

            //when
            List<LocationsListResponse> found = locationsRepository.findAllByGroups_GroupId(failGroupId);

            //then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Location ID가 존재하더라도 Group ID가 일치하지 않으면 조회 실패")
        void findByLocationIdAndGroups_GroupId_mismatchGroupId() {
            //given
            Long locationId = 1L;
            Long failGroupId = 999L;

            // when
            Optional<Locations> found = locationsRepository.findByLocationIdAndGroups_GroupId(locationId, failGroupId);

            assertThat(found).isEmpty();
        }
        
        @Test
        @DisplayName("그룹의 아이디와 location 이름 둘 다 해당하는 Location이 있는지 확인 실패")
        void existsByGroups_GroupIdAndLocationName_fail() {
            Long groupId = 1L;
            String locationName = "no";

            boolean fail = locationsRepository.existsByGroups_GroupIdAndLocationName(groupId, locationName);

            assertThat(fail).isFalse();
        }
    }
}