package com.insighton.core.location.service;

import com.insighton.core.groups.entity.Group;
import com.insighton.core.groups.service.impl.GroupServiceImpl;
import com.insighton.core.location.dto.request.LocationCreateRequest;
import com.insighton.core.location.dto.request.LocationUpdateRequest;
import com.insighton.core.location.dto.response.LocationListResponse;
import com.insighton.core.location.dto.response.LocationResponse;
import com.insighton.core.location.entity.Location;
import com.insighton.core.location.exception.EmptyValueException;
import com.insighton.core.location.exception.LocationAlreadyException;
import com.insighton.core.location.exception.LocationNotFoundException;
import com.insighton.core.location.repository.LocationRepository;
import com.insighton.core.location.service.impl.LocationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {
    @Mock
    private LocationRepository locationRepository;

    @Mock
    private GroupServiceImpl groupsService;

    @InjectMocks
    private LocationServiceImpl locationsService;


    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("Location 생성 성공")
        void createLocation_success() {

            Group group = Group.builder()
                    .name("Test Group")
                    .build();

            LocationCreateRequest locationsRequest = new LocationCreateRequest("Test Location", Location.AutoControlMode.SUGGESTION);


            locationsService.createLocation(group, locationsRequest);

            verify(locationRepository, times(1)).saveAndFlush(any(Location.class));

        }

        @Test
        @DisplayName("Location List 조회 성공")
        void getListLocation_success() {
            // given
            Long groupId = 1L;

            LocationListResponse response = new LocationListResponse(1L, "Name", Location.AutoControlMode.SUGGESTION);

            List<LocationListResponse> mockList = List.of(response);

            given(locationRepository.findAllByGroupGroupId(groupId)).willReturn(mockList);

            // when
            List<LocationListResponse> locationListResponse = locationsService.getLocationList(groupId);

            // then
            assertThat(locationListResponse).hasSize(1);
            assertThat(locationListResponse.getFirst().locationName()).isEqualTo("Name");
            assertThat(locationListResponse.getFirst().autoControlMode()).isEqualTo(Location.AutoControlMode.SUGGESTION);

            verify(locationRepository, times(1)).findAllByGroupGroupId(groupId);
        }

        @Test
        @DisplayName("Location 상세 조회 성공")
        void getLocations_success() {
            // given
            Long groupId = 1L;
            Long locationId = 1L;
            Group group = Group.builder()
                    .name("Test Group")
                    .build();

            Location location = new Location(group, "name", Location.AutoControlMode.SUGGESTION);

            given(locationRepository.findByLocationIdAndGroupGroupId(locationId, groupId)).willReturn(Optional.of(location));

            // when
            LocationResponse found = locationsService.getLocation(locationId, groupId);

            // then
            assertThat(found).isNotNull();
            assertThat(found.locationName()).isEqualTo("name");
            assertThat(found.groupId()).isEqualTo(1L);
            assertThat(found.autoControlMode()).isEqualTo(Location.AutoControlMode.SUGGESTION);

            verify(locationRepository, times(1)).findByLocationIdAndGroupGroupId(locationId, groupId);
        }

        @Test
        @DisplayName("AI 모드 변경 성공")
        void toggleAutoControlMode_success() {
            // given
            Long groupId = 1L;
            Long locationId = 1L;
            Group group = Group.builder()
                    .name("Test Group")
                    .build();

            Location location = new Location(group, "name", Location.AutoControlMode.SUGGESTION);

            given(locationRepository.findByLocationIdAndGroupGroupId(locationId, groupId)).willReturn(Optional.of(location));

            // when
            locationsService.toggleAutoControlMode(locationId, groupId);

            //then
            assertThat(location.getAutoControlMode()).isEqualTo(Location.AutoControlMode.AI_DIRECT);

            verify(locationRepository, times(1)).findByLocationIdAndGroupGroupId(locationId, groupId);
        }

        @Test
        @DisplayName("location 이름 변경 성공")
        void updateLocationName_success() {
            // given
            Long groupId = 1L;
            Long locationId = 1L;
            Group group = Group.builder()
                    .name("Test Group")
                    .build();

            Location location = new Location(group, "name", Location.AutoControlMode.SUGGESTION);

            given(locationRepository.findByLocationIdAndGroupGroupId(locationId, groupId)).willReturn(Optional.of(location));

            LocationUpdateRequest request = new LocationUpdateRequest("new");

            // when
            locationsService.updateName(locationId, groupId, request);

            //then
            assertThat(location.getLocationName()).isEqualTo("new");

            verify(locationRepository, times(1)).findByLocationIdAndGroupGroupId(locationId, groupId);
        }

        @Test
        @DisplayName("location 삭제 성공")
        void deleteLocation_success() {
            // given
            Long groupId = 1L;
            Long targetLocationId = 1L;
            Group group = Group.builder()
                    .name("Test Group")
                    .build();

            Location targetLocation = new Location(group, "name", Location.AutoControlMode.SUGGESTION);

            given(locationRepository.findByLocationIdAndGroupGroupId(targetLocationId, groupId)).willReturn(Optional.of(targetLocation));

            // when
            locationsService.deleteLocation(targetLocationId, groupId);

            // then
            verify(locationRepository, times(1)).delete(targetLocation);
        }

        @Test
        @DisplayName("group ID에 해당하는 모든 location 삭제 성공")
        void deleteLocationAll_success() {
            // given
            Long groupId = 1L;

            willDoNothing().given(locationRepository).deleteAllByGroupGroupId(groupId);

            // when
            locationsService.deleteLocationAll(groupId);

            // then
            verify(locationRepository, times(1)).deleteAllByGroupGroupId(groupId);
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureOrEmpty {
        @Test
        @DisplayName("location 생성 실패 - 중복된 이름의 location이 존재할 때")
        void createLocation_fail() {
            // given
            Group group = Group.builder()
                    .name("Test Group")
                    .build();

            LocationCreateRequest locationsRequest = new LocationCreateRequest("Test Location", Location.AutoControlMode.SUGGESTION);

            given(locationRepository.existsByGroupGroupIdAndLocationName(group.getGroupId(), locationsRequest.locationName())).willReturn(true);


            // when & then
            assertThatThrownBy(() -> locationsService.createLocation(group, locationsRequest))
                    .isInstanceOf(LocationAlreadyException.class);
        }

        @Test
        @DisplayName("Location List 조회 성공 - 데이터가 없는 경우 빈 리스트 반환")
        void getListLocation_empty() {
            // given
            Long nonExistentGroupId = 999L;
            given(locationRepository.findAllByGroupGroupId(nonExistentGroupId)).willReturn(List.of());

            // when
            List<LocationListResponse> result = locationsService.getLocationList(nonExistentGroupId);

            // then
            assertThat(result).isEmpty();
            verify(locationRepository, times(1)).findAllByGroupGroupId(nonExistentGroupId);
        }

        @Test
        @DisplayName("location 상세 정보 조회 실패 - group에 Location이 존재하지 않을 때")
        void getLocation_fail() {
            // given
            Long groupId = 1L;
            Long locationId = 999L;

            given(locationRepository.findByLocationIdAndGroupGroupId(locationId, groupId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> locationsService.getLocation(locationId, groupId))
                    .isInstanceOf(LocationNotFoundException.class);
        }

        @Test
        @DisplayName("AI 모드 변경 실패 - group에 Location이 존재하지 않을 때")
        void toggleAutoControlMode_fail() {
            // given
            Long groupId = 1L;
            Long locationId = 999L;

            given(locationRepository.findByLocationIdAndGroupGroupId(locationId, groupId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> locationsService.toggleAutoControlMode(locationId, groupId))
                    .isInstanceOf(LocationNotFoundException.class);
        }

        @Test
        @DisplayName("location 이름 수정 실패 - 새로운 이름이 비어있을 때")
        void updateName_fail() {
            // given
            Long groupId = 1L;
            Group group = Group.builder()
                    .name("Test Group")
                    .build();
            Long locationId = 1L;
            Location targetLocation = new Location(group, "name", Location.AutoControlMode.SUGGESTION);
            LocationUpdateRequest request = new LocationUpdateRequest("");
            given(locationRepository.findByLocationIdAndGroupGroupId(locationId, groupId)).willReturn(Optional.of(targetLocation));

            // when & then
            assertThatThrownBy(() -> locationsService.updateName(locationId, groupId, request))
                    .isInstanceOf(EmptyValueException.class);
        }

        @Test
        @DisplayName("Location 삭제 실패 - 존재하지 않는 locationID")
        void deleteLocation_fail() {
            // given
            Long groupId = 1L;
            Long locationId = 999L;

            given(locationRepository.findByLocationIdAndGroupGroupId(locationId, groupId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> locationsService.deleteLocation(locationId, groupId))
                    .isInstanceOf(LocationNotFoundException.class);
        }

    }
}