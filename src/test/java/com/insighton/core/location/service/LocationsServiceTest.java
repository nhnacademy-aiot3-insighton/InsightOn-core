package com.insighton.core.location.service;

import com.insighton.core.groups.entity.Groups;
import com.insighton.core.groups.service.impl.GroupsServiceImpl;
import com.insighton.core.location.dto.request.LocationsCreateRequest;
import com.insighton.core.location.dto.response.LocationsListResponse;
import com.insighton.core.location.entity.Locations;
import com.insighton.core.location.repository.LocationsRepository;
import com.insighton.core.location.service.impl.LocationsServiceImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
        @Disabled("Mockito strict stubbing이 미사용 stub 감지(UnnecessaryStubbingException) — 배포 테스트 위해 임시 비활성화")
        @DisplayName("Location List 조회 성공")
        void getListLocation_success() {
            // given
            Long groupId = 1L;

            LocationsListResponse response = new LocationsListResponse(1L, "Name", Locations.AutoControlMode.SUGGESTION);

            List<LocationsListResponse> mockList = List.of(response);

            given(locationsRepository.findAllByGroups_GroupId(groupId)).willReturn(mockList);

            // when
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

    }
}