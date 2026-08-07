package com.insighton.core.location.repository;

import com.insighton.core.domain.location.dto.response.LocationListResponse;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.repository.LocationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test") // 1. test 프로파일(application-test.properties)을 읽어오도록 지정
@Sql(scripts = "/location-test.sql")
class LocationRepositoryTest {

    @Autowired
    LocationRepository locationRepository;

    @Nested
    @DisplayName("성공")
    class success {
        @Test
        @DisplayName("그룹의 아이디로 해당하는 모든 location 조회 성공")
        void findAllByGroupGroupId_success() {
            Long groupId = 1L;

            List<LocationListResponse> found = locationRepository.findAllByGroupGroupId(groupId);

            assertThat(found).hasSize(1);
            assertThat(found.getFirst().locationName()).isEqualTo("test-name");
        }

        @Test
        @DisplayName("그룹의 아이디와  location 아이디 둘 다 해당하는 location의 상세 정보 조회 성공")
        void findByLocationIdAndGroupGroupId_success() {
            Long groupId = 1L;
            Long locationId = 1L;

            Optional<Location> found = locationRepository.findByLocationIdAndGroupGroupId(locationId, groupId);

            assertThat(found).isPresent();
            assertThat(found.get().getLocationName()).isEqualTo("test-name");
        }

        @Test
        @DisplayName("그룹의 아이디와 location 이름 둘 다 해당하는 Location이 있는지 확인 성공")
        void existsByGroupGroupIdAndLocationName_success() {
            Long groupId = 1L;
            String locationName = "test-name";

            boolean success = locationRepository.existsByGroupGroupIdAndLocationName(groupId, locationName);

            assertThat(success).isTrue();
        }

        @Test
        @DisplayName("그룹 ID에 해당하는 모든 location 삭제 성공")
        void deleteByGroups_GroupId_success() {
            // given
            Long groupId = 1L;

            // when
            locationRepository.deleteAllByGroupGroupId(groupId);

            // then
            List<LocationListResponse> found = locationRepository.findAllByGroupGroupId(groupId);
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("실패")
    class Failure {
        @Test
        @DisplayName("존재하지 않는 그룹 ID로 location 조회 시 빈 리스트 반환")
        void findAllByGroupGroupId_empty() {
            // given
            Long failGroupId = 999L;

            //when
            List<LocationListResponse> found = locationRepository.findAllByGroupGroupId(failGroupId);

            //then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Location ID가 존재하더라도 Group ID가 일치하지 않으면 조회 실패")
        void findByLocationIdAndGroupGroupId_mismatchGroupId() {
            //given
            Long locationId = 1L;
            Long failGroupId = 999L;

            // when
            Optional<Location> found = locationRepository.findByLocationIdAndGroupGroupId(locationId, failGroupId);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("그룹의 아이디와 location 이름 둘 다 해당하는 Location이 있는지 확인 실패")
        void existsByGroupGroupIdAndLocationName_fail() {
            Long groupId = 1L;
            String locationName = "no";

            boolean fail = locationRepository.existsByGroupGroupIdAndLocationName(groupId, locationName);

            assertThat(fail).isFalse();
        }
    }
}