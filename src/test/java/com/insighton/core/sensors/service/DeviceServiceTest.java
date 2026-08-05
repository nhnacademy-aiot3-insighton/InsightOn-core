package com.insighton.core.sensors.service;

import com.insighton.core.device_attributes.repository.DeviceAttributeRepository;
import com.insighton.core.gateway.entity.Gateway;
import com.insighton.core.gateway.exception.GatewayNotFoundException;
import com.insighton.core.gateway.repository.GatewayRepository;
import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groupmember.entity.GroupMembers.GroupRole;
import com.insighton.core.groupmember.service.GroupMembersService;
import com.insighton.core.groups.entity.Groups;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.groups.repository.GroupsRepository;
import com.insighton.core.location.repository.LocationsRepository;
import com.insighton.core.mqtt.cache.DeviceLookupCacheService;
import com.insighton.core.mqtt.cache.dto.DeviceCacheEntry;
import com.insighton.core.sensors.entity.Device;
import com.insighton.core.sensors.exception.DeviceNotFoundException;
import com.insighton.core.sensors.exception.InvalidDeviceValueException;
import com.insighton.core.sensors.repository.DeviceRepository;
import com.insighton.core.sensors.service.impl.DeviceServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.insighton.core.location.entity.Locations;
import com.insighton.core.location.exception.LocationNotFoundException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock private DeviceRepository deviceRepository;
    @Mock private DeviceAttributeRepository deviceAttributeRepository;
    @Mock private DeviceLookupCacheService deviceLookupCacheService;
    @Mock private GroupMembersService groupMembersService;
    @Mock private GatewayRepository gatewayRepository;
    @Mock private GroupsRepository groupsRepository;
    @Mock private LocationsRepository locationsRepository;

    @InjectMocks
    private DeviceServiceImpl deviceService;

    @Test
    @DisplayName("autoProvision - 신규 EUI면 새 디바이스를 생성한다")
    void autoProvision_신규디바이스_생성() {
        given(deviceRepository.findByDeviceEui("EUI-001")).willReturn(Optional.empty());
        given(gatewayRepository.findById(10L)).willReturn(Optional.of(mock(Gateway.class)));
        given(groupsRepository.findById(5L)).willReturn(Optional.of(mock(Groups.class)));

        Device saved = Device.builder()
                .deviceId(100L)
                .deviceEui("EUI-001")
                .build();
        given(deviceRepository.save(any(Device.class))).willReturn(saved);

        DeviceCacheEntry result = deviceService.autoProvision(10L, 5L, "EUI-001", "센서", Set.of("co2"));

        assertThat(result.deviceId()).isEqualTo(100L);
        verify(deviceLookupCacheService).populate(any(DeviceCacheEntry.class));
        verify(deviceAttributeRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("autoProvision - 이미 존재하는 EUI면 재조회 후 캐시만 복구한다 (신규 저장 안 함)")
    void autoProvision_기존EUI_캐시복구만() {
        Groups group = mock(Groups.class);
        given(group.getGroupId()).willReturn(5L); // 요청 groupId(5L)와 일치해야 통과

        // gatewaysId는 null로 둬도 됨 - null이면 파라미터로 받은 gatewayId(10L)로 대체되는 로직이라 스텁 불필요
        Device existing = Device.builder().deviceId(1L).deviceEui("EUI-001").groupId(group).build();
        given(deviceRepository.findByDeviceEui("EUI-001")).willReturn(Optional.of(existing));

        DeviceCacheEntry result = deviceService.autoProvision(10L, 5L, "EUI-001", "센서", Set.of("co2"));

        assertThat(result.deviceId()).isEqualTo(1L);
        assertThat(result.gatewayId()).isEqualTo(10L); // gatewaysId null -> 파라미터로 받은 값으로 대체됨
        verify(deviceRepository, never()).save(any());
        verify(deviceLookupCacheService).populate(any(DeviceCacheEntry.class));
    }

    @Test
    @DisplayName("autoProvision - 이미 다른 그룹에 등록된 EUI면 InvalidDeviceValueException")
    void autoProvision_다른그룹EUI_충돌() {
        Groups otherGroup = mock(Groups.class);
        given(otherGroup.getGroupId()).willReturn(999L); // 요청 groupId(5L)와 다름

        Device existing = Device.builder().deviceId(1L).deviceEui("EUI-001").groupId(otherGroup).build();
        given(deviceRepository.findByDeviceEui("EUI-001")).willReturn(Optional.of(existing));

        assertThrows(InvalidDeviceValueException.class,
                () -> deviceService.autoProvision(10L, 5L, "EUI-001", "센서", Set.of("co2")));

        verify(deviceLookupCacheService, never()).populate(any());
    }

    @Test
    @DisplayName("autoProvision - 게이트웨이가 없으면 GatewayNotFoundException")
    void autoProvision_게이트웨이없음() {
        given(deviceRepository.findByDeviceEui(anyString())).willReturn(Optional.empty());
        given(gatewayRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(GatewayNotFoundException.class,
                () -> deviceService.autoProvision(999L, 5L, "EUI-001", "센서", Set.of()));
    }

    @Test
    @DisplayName("getDeviceById - 다른 그룹 소속이면 예외")
    void 조회_다른그룹이면_예외() {
        Groups group = mock(Groups.class);
        given(group.getGroupId()).willReturn(5L);
        Device device = Device.builder().deviceId(1L).groupId(group).build();

        given(deviceRepository.findById(1L)).willReturn(Optional.of(device));
        given(groupMembersService.validateGroupMembers(5L, 999L))
                .willThrow(com.insighton.core.groupmember.exception
                        .GroupMemberNotFoundException.byMemberIdAndGroupId(999L, 5L));

        assertThrows(com.insighton.core.groupmember.exception.GroupMemberNotFoundException.class,
                () -> deviceService.getDeviceById(999L, 1L));
    }

    @Test
    @DisplayName("updateDeviceName - MEMBER 권한이면 NoPermissionException")
    void 이름수정_권한없음() {
        Groups group = mock(Groups.class);
        given(group.getGroupId()).willReturn(5L);
        Device device = Device.builder().deviceId(1L).groupId(group).build();

        given(deviceRepository.findById(1L)).willReturn(Optional.of(device));
        GroupMembers member = GroupMembers.builder().userId(1L).groupRole(GroupRole.MEMBER).build();
        given(groupMembersService.validateGroupMembers(5L, 1L)).willReturn(member);

        assertThrows(NoPermissionException.class,
                () -> deviceService.updateDeviceName(1L, 1L, "새이름"));
    }

    @Test
    @DisplayName("updateDeviceName - 빈 문자열이면 InvalidDeviceValueException, 리포지토리 호출 전에 걸러짐")
    void 이름수정_빈값() {
        assertThrows(InvalidDeviceValueException.class,
                () -> deviceService.updateDeviceName(1L, 1L, "   "));
        verify(deviceRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("deleteDevice - 없는 디바이스면 DeviceNotFoundException")
    void 삭제_없는디바이스() {
        given(deviceRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(DeviceNotFoundException.class,
                () -> deviceService.deleteDevice(1L, 999L));
    }

    @Test
    @DisplayName("deleteAll - EUI가 null인 디바이스(ACTUATOR성 데이터)는 evict 대상에서 제외")
    void 전체삭제_null_EUI_필터링() {
        Device withEui = Device.builder().deviceId(1L).deviceEui("EUI-001").build();
        Device withoutEui = Device.builder().deviceId(2L).deviceEui(null).build();

        given(groupMembersService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMembers.builder().userId(1L).groupRole(GroupRole.MANAGER).build());
        given(deviceRepository.findByGroupId(5L)).willReturn(List.of(withEui, withoutEui));

        deviceService.deleteAll(1L, 5L);

        verify(deviceLookupCacheService, times(1)).evict("EUI-001");
        verify(deviceAttributeRepository).deleteByGroupId(5L);
        verify(deviceRepository).deleteAll(List.of(withEui, withoutEui));
    }

    @Test
    @DisplayName("updateDevice - 위치만 수정, EUI가 있으면 캐시도 갱신")
    void 업데이트_위치만_수정_캐시갱신() {
        Groups group = mock(Groups.class);
        given(group.getGroupId()).willReturn(5L);
        Gateway gateway = mock(Gateway.class);
        given(gateway.getGatewayId()).willReturn(10L); // deviceEui != null 분기에서 실제로 호출됨
        Device device = Device.builder()
                .deviceId(1L).groupId(group).gatewaysId(gateway).deviceEui("EUI-001").build();

        Locations newLocation = mock(Locations.class); // getter 스텁 불필요 - newLocationId 파라미터를 그대로 씀

        given(deviceRepository.findById(1L)).willReturn(Optional.of(device));
        given(groupMembersService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMembers.builder().userId(1L).groupRole(GroupRole.MANAGER).build());
        given(locationsRepository.findById(20L)).willReturn(Optional.of(newLocation));

        deviceService.updateDevice(1L, 1L, 20L, null);

        assertThat(device.getLocation()).isEqualTo(newLocation); // getLocationsId() -> getLocation()
        verify(deviceLookupCacheService).populate(any(DeviceCacheEntry.class));
    }

    @Test
    @DisplayName("updateDevice - 위치/이름 둘 다 수정 성공 (EUI 없으면 캐시 갱신 스킵)")
    void 업데이트_위치_이름_둘다_성공() {
        Groups group = mock(Groups.class);
        given(group.getGroupId()).willReturn(5L);
        Device device = Device.builder().deviceId(1L).groupId(group).deviceEui(null).build();

        Locations newLocation = mock(Locations.class); // 여기도 getter 스텁 불필요

        given(deviceRepository.findById(1L)).willReturn(Optional.of(device));
        given(groupMembersService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMembers.builder().userId(1L).groupRole(GroupRole.MANAGER).build());
        given(locationsRepository.findById(20L)).willReturn(Optional.of(newLocation));

        deviceService.updateDevice(1L, 1L, 20L, "새 이름");

        assertThat(device.getLocation()).isEqualTo(newLocation);
        assertThat(device.getDeviceName()).isEqualTo("새 이름");
        verify(deviceLookupCacheService, never()).populate(any()); // EUI null이라 캐시 갱신 안 함
    }
    @Test
    @DisplayName("updateDevice - 이름만 수정, 위치는 그대로")
    void 업데이트_이름만_수정() {
        Groups group = mock(Groups.class);
        given(group.getGroupId()).willReturn(5L);
        Device device = Device.builder().deviceId(1L).groupId(group).deviceName("기존 이름").build();

        given(deviceRepository.findById(1L)).willReturn(Optional.of(device));
        given(groupMembersService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMembers.builder().userId(1L).groupRole(GroupRole.MANAGER).build());

        deviceService.updateDevice(1L, 1L, null, "새 이름");

        assertThat(device.getDeviceName()).isEqualTo("새 이름");
        verify(locationsRepository, never()).findById(any());
        verify(deviceLookupCacheService, never()).populate(any());
    }


    @Test
    @DisplayName("updateDevice - 위치/이름 둘 다 null이면 InvalidDeviceValueException")
    void 업데이트_둘다_null이면_거부() {
        // 구현에 이 가드가 없다면 이 테스트는 삭제하세요
        assertThrows(InvalidDeviceValueException.class,
                () -> deviceService.updateDevice(1L, 1L, null, null));
        verify(deviceRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("updateDevice - 이름이 빈 문자열이면 InvalidDeviceValueException")
    void 업데이트_빈이름_거부() {
        Groups group = mock(Groups.class);
        given(group.getGroupId()).willReturn(5L);
        Device device = Device.builder().deviceId(1L).groupId(group).build();

        given(deviceRepository.findById(1L)).willReturn(Optional.of(device));
        given(groupMembersService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMembers.builder().userId(1L).groupRole(GroupRole.MANAGER).build());

        assertThrows(InvalidDeviceValueException.class,
                () -> deviceService.updateDevice(1L, 1L, null, "   "));
    }

    @Test
    @DisplayName("updateDevice - 없는 디바이스면 DeviceNotFoundException")
    void 업데이트_없는디바이스() {
        given(deviceRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(DeviceNotFoundException.class,
                () -> deviceService.updateDevice(1L, 999L, 20L, null));
    }

    @Test
    @DisplayName("updateDevice - 없는 위치면 LocationNotFoundException")
    void 업데이트_없는위치() {
        Groups group = mock(Groups.class);
        given(group.getGroupId()).willReturn(5L);
        Device device = Device.builder().deviceId(1L).groupId(group).build();

        given(deviceRepository.findById(1L)).willReturn(Optional.of(device));
        given(groupMembersService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMembers.builder().userId(1L).groupRole(GroupRole.MANAGER).build());
        given(locationsRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(LocationNotFoundException.class,
                () -> deviceService.updateDevice(1L, 1L, 999L, null));
    }

    @Test
    @DisplayName("updateDevice - MEMBER 권한이면 NoPermissionException")
    void 업데이트_권한없음() {
        Groups group = mock(Groups.class);
        given(group.getGroupId()).willReturn(5L);
        Device device = Device.builder().deviceId(1L).groupId(group).build();

        given(deviceRepository.findById(1L)).willReturn(Optional.of(device));
        given(groupMembersService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMembers.builder().userId(1L).groupRole(GroupRole.MEMBER).build());

        assertThrows(NoPermissionException.class,
                () -> deviceService.updateDevice(1L, 1L, 20L, "새 이름"));
    }
}