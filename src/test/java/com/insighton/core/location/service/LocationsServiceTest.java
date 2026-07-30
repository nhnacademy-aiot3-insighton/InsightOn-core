package com.insighton.core.location.service;

import com.insighton.core.groups.entity.Groups;
import com.insighton.core.groups.service.impl.GroupsServiceImpl;
import com.insighton.core.location.dto.request.LocationsCreateRequest;
import com.insighton.core.location.dto.request.LocationsUpdateRequest;
import com.insighton.core.location.dto.response.LocationsListResponse;
import com.insighton.core.location.dto.response.LocationsResponse;
import com.insighton.core.location.entity.Locations;
import com.insighton.core.location.exception.EmptyValueException;
import com.insighton.core.location.exception.LocationAlreadyException;
import com.insighton.core.location.exception.LocationNotFoundException;
import com.insighton.core.location.repository.LocationsRepository;
import com.insighton.core.location.service.impl.LocationsServiceImpl;
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
class LocationsServiceTest {
    @Mock
    private LocationsRepository locationsRepository;

    @Mock
    private GroupsServiceImpl groupsService;

    @InjectMocks
    private LocationsServiceImpl locationsService;


    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        @DisplayName("Location 생성 성공")
        void createLocation_success() {

            Groups groups = Groups.builder()
                    .name("Test Group")
                    .build();

            LocationsCreateRequest locationsRequest = new LocationsCreateRequest("Test Location", Locations.AutoControlMode.SUGGESTION);


            locationsService.createLocation(groups, locationsRequest);

            verify(locationsRepository, times(1)).save(any(Locations.class));

        }

        @Test
        @DisplayName("Location List 조회 성공")
        void getListLocation_success() {
            // given
            Long groupId = 1L;

            LocationsListResponse response = new LocationsListResponse(1L, "Name", Locations.AutoControlMode.SUGGESTION);

            List<LocationsListResponse> mockList = List.of(response);

            given(locationsRepository.findAllByGroups_GroupId(groupId)).willReturn(mockList);

            // when
            List<LocationsListResponse> locationsListResponses = locationsService.getLocationList(groupId);

            // then
            assertThat(locationsListResponses).hasSize(1);
            assertThat(locationsListResponses.getFirst().locationName()).isEqualTo("Name");
            assertThat(locationsListResponses.getFirst().autoControlMode()).isEqualTo(Locations.AutoControlMode.SUGGESTION);

            verify(locationsRepository, times(1)).findAllByGroups_GroupId(groupId);
        }

        @Test
        @DisplayName("Location 상세 조회 성공")
        void getLocations_success() {
            // given
            Long groupId = 1L;
            Long locationId = 1L;
            Groups groups = Groups.builder()
                    .name("Test Group")
                    .build();

            Locations locations = new Locations(groups, "name", Locations.AutoControlMode.SUGGESTION);

            given(locationsRepository.findByLocationIdAndGroups_GroupId(locationId, groupId)).willReturn(Optional.of(locations));

            // when
            LocationsResponse found = locationsService.getLocation(locationId, groupId);

            // then
            assertThat(found).isNotNull();
            assertThat(found.locationName()).isEqualTo("name");
            assertThat(found.groupId()).isEqualTo(1L);
            assertThat(found.autoControlMode()).isEqualTo(Locations.AutoControlMode.SUGGESTION);

            verify(locationsRepository, times(1)).findByLocationIdAndGroups_GroupId(locationId, groupId);
        }

        @Test
        @DisplayName("AI 모드 변경 성공")
        void toggleAutoControlMode_success() {
            // given
            Long groupId = 1L;
            Long locationId = 1L;
            Groups groups = Groups.builder()
                    .name("Test Group")
                    .build();

            Locations locations = new Locations(groups, "name", Locations.AutoControlMode.SUGGESTION);

            given(locationsRepository.findByLocationIdAndGroups_GroupId(locationId, groupId)).willReturn(Optional.of(locations));

            // when
            locationsService.toggleAutoControlMode(locationId, groupId);

            //then
            assertThat(locations.getAutoControlMode()).isEqualTo(Locations.AutoControlMode.AI_DIRECT);

            verify(locationsRepository, times(1)).findByLocationIdAndGroups_GroupId(locationId, groupId);
        }

        @Test
        @DisplayName("location 이름 변경 성공")
        void updateLocationName_success() {
            // given
            Long groupId = 1L;
            Long locationId = 1L;
            Groups groups = Groups.builder()
                    .name("Test Group")
                    .build();

            Locations locations = new Locations(groups, "name", Locations.AutoControlMode.SUGGESTION);

            given(locationsRepository.findByLocationIdAndGroups_GroupId(locationId, groupId)).willReturn(Optional.of(locations));

            LocationsUpdateRequest request = new LocationsUpdateRequest("new");

            // when
            locationsService.updateName(locationId, groupId, request);

            //then
            assertThat(locations.getLocationName()).isEqualTo("new");

            verify(locationsRepository, times(1)).findByLocationIdAndGroups_GroupId(locationId, groupId);
        }

        @Test
        @DisplayName("location 삭제 성공")
        void deleteLocation_success() {
            // given
            Long groupId = 1L;
            Long targetLocationId = 1L;
            Groups groups = Groups.builder()
                    .name("Test Group")
                    .build();

            Locations targetLocations = new Locations(groups, "name", Locations.AutoControlMode.SUGGESTION);

            given(locationsRepository.findByLocationIdAndGroups_GroupId(targetLocationId, groupId)).willReturn(Optional.of(targetLocations));

            // when
            locationsService.deleteLocation(targetLocationId, groupId);

            // then
            verify(locationsRepository, times(1)).delete(targetLocations);
        }

        @Test
        @DisplayName("group ID에 해당하는 모든 location 삭제 성공")
        void deleteLocationAll_success() {
            // given
            Long groupId = 1L;

            willDoNothing().given(locationsRepository).deleteAllByGroups_GroupId(groupId);

            // when
            locationsService.deleteLocationAll(groupId);

            // then
            verify(locationsRepository, times(1)).deleteAllByGroups_GroupId(groupId);
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureOrEmpty {
        @Test
        @DisplayName("location 생성 실패 - 중복된 이름의 location이 존재할 때")
        void createLocation_fail() {
            // given
            Groups groups = Groups.builder()
                    .name("Test Group")
                    .build();

            LocationsCreateRequest locationsRequest = new LocationsCreateRequest("Test Location", Locations.AutoControlMode.SUGGESTION);

            given(locationsRepository.existsByGroups_GroupIdAndLocationName(groups.getGroupId(), locationsRequest.locationName())).willReturn(true);


            // when & then
            assertThatThrownBy(() -> locationsService.createLocation(groups, locationsRequest))
                    .isInstanceOf(LocationAlreadyException.class);
        }

        @Test
        @DisplayName("Location List 조회 성공 - 데이터가 없는 경우 빈 리스트 반환")
        void getListLocation_empty() {
            // given
            Long nonExistentGroupId = 999L;
            given(locationsRepository.findAllByGroups_GroupId(nonExistentGroupId)).willReturn(List.of());

            // when
            List<LocationsListResponse> result = locationsService.getLocationList(nonExistentGroupId);

            // then
            assertThat(result).isEmpty();
            verify(locationsRepository, times(1)).findAllByGroups_GroupId(nonExistentGroupId);
        }

        @Test
        @DisplayName("location 상세 정보 조회 실패 - group에 Location이 존재하지 않을 때")
        void getLocation_fail() {
            // given
            Long groupId = 1L;
            Long locationId = 999L;

            given(locationsRepository.findByLocationIdAndGroups_GroupId(locationId, groupId)).willReturn(Optional.empty());

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

            given(locationsRepository.findByLocationIdAndGroups_GroupId(locationId, groupId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> locationsService.toggleAutoControlMode(locationId, groupId))
                    .isInstanceOf(LocationNotFoundException.class);
        }

        @Test
        @DisplayName("location 이름 수정 실패 - 새로운 이름이 비어있을 때")
        void updateName_fail() {
            // given
            Long groupId = 1L;
            Groups groups = Groups.builder()
                    .name("Test Group")
                    .build();
            Long locationId = 1L;
            Locations targetLocations = new Locations(groups, "name", Locations.AutoControlMode.SUGGESTION);
            LocationsUpdateRequest request = new LocationsUpdateRequest("");
            given(locationsRepository.findByLocationIdAndGroups_GroupId(locationId, groupId)).willReturn(Optional.of(targetLocations));

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

            given(locationsRepository.findByLocationIdAndGroups_GroupId(locationId, groupId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> locationsService.deleteLocation(locationId, groupId))
                    .isInstanceOf(LocationNotFoundException.class);
        }

    }
}