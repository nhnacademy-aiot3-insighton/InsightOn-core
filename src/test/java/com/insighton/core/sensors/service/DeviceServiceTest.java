//package com.insighton.core.sensors.service;
//
//import com.insighton.core.device_attributes.repository.DeviceAttributeRepository;
//import com.insighton.core.gateway.entity.Gateway;
//import com.insighton.core.gateway.exception.GatewayNotFoundException;
//import com.insighton.core.gateway.repository.GatewayRepository;
//import com.insighton.core.groupmember.entity.GroupMembers;
//import com.insighton.core.groupmember.entity.GroupMembers.GroupRole;
//import com.insighton.core.groupmember.service.GroupMembersService;
//import com.insighton.core.groups.entity.Groups;
//import com.insighton.core.groups.exception.NoPermissionException;
//import com.insighton.core.groups.repository.GroupsRepository;
//import com.insighton.core.location.repository.LocationsRepository;
//import com.insighton.core.mqtt.cache.DeviceLookupCacheService;
//import com.insighton.core.mqtt.cache.dto.DeviceCacheEntry;
//import com.insighton.core.sensors.entity.Device;
//import com.insighton.core.sensors.exception.DeviceNotFoundException;
//import com.insighton.core.sensors.exception.InvalidDeviceValueException;
//import com.insighton.core.sensors.repository.DeviceRepository;
//import com.insighton.core.sensors.service.impl.DeviceServiceImpl;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.lang.reflect.Field;
//import java.util.List;
//import java.util.Optional;
//import java.util.Set;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class DeviceServiceTest {
//
//    @Mock private DeviceRepository deviceRepository;
//    @Mock private DeviceAttributeRepository deviceAttributeRepository;
//    @Mock private DeviceLookupCacheService deviceLookupCacheService;
//    @Mock private GroupMembersService groupMembersService;
//    @Mock private GatewayRepository gatewayRepository;
//    @Mock private GroupsRepository groupsRepository;
//    @Mock private LocationsRepository locationsRepository;
//
//    @InjectMocks
//    private DeviceServiceImpl deviceService;
//
//    // ID가 자동 생성 전략이라 테스트에서 리플렉션으로 직접 주입
//    private void setId(Object entity, String fieldName, Object value) throws Exception {
//        Field field = entity.getClass().getDeclaredField(fieldName);
//        field.setAccessible(true);
//        field.set(entity, value);
//    }
//
//    private GroupMembers manager(Long groupMemberId) {
//        GroupMembers member = GroupMembers.builder().userId(1L).groupRole(GroupRole.MANAGER).build();
//        return member;
//    }
//
//    @Test
//    @DisplayName("autoProvision - 신규 EUI면 새 디바이스를 생성한다")
//    void autoProvision_신규디바이스_생성() {
//        given(deviceRepository.findByDeviceEui("EUI-001")).willReturn(Optional.empty());
//        given(gatewayRepository.findById(10L)).willReturn(Optional.of(mock(Gateway.class)));
//        given(groupsRepository.findById(5L)).willReturn(Optional.of(mock(Groups.class)));
//
//        Device saved = Device.builder()
//                .deviceId(100L)
//                .deviceEui("EUI-001")
//                .build();
//        given(deviceRepository.save(any(Device.class))).willReturn(saved);
//
//        DeviceCacheEntry result = deviceService.autoProvision(10L, 5L, "EUI-001", "센서", Set.of("co2"));
//
//        assertThat(result.deviceId()).isEqualTo(100L);
//        verify(deviceLookupCacheService).populate(any(DeviceCacheEntry.class));
//        verify(deviceAttributeRepository).saveAll(anyList());
//    }
//
//    @Test
//    @DisplayName("autoProvision - 이미 존재하는 EUI면 재조회 후 캐시만 복구한다 (신규 저장 안 함)")
//    void autoProvision_기존EUI_캐시복구만() {
//        Device existing = Device.builder().deviceId(1L).deviceEui("EUI-001").build();
//        given(deviceRepository.findByDeviceEui("EUI-001")).willReturn(Optional.of(existing));
//
//        DeviceCacheEntry result = deviceService.autoProvision(10L, 5L, "EUI-001", "센서", Set.of("co2"));
//
//        assertThat(result.deviceId()).isEqualTo(1L);
//        verify(deviceRepository, never()).save(any());
//        verify(deviceLookupCacheService).populate(any(DeviceCacheEntry.class));
//    }
//
//    @Test
//    @DisplayName("autoProvision - 게이트웨이가 없으면 GatewayNotFoundException")
//    void autoProvision_게이트웨이없음() {
//        given(deviceRepository.findByDeviceEui(anyString())).willReturn(Optional.empty());
//        given(gatewayRepository.findById(999L)).willReturn(Optional.empty());
//
//        assertThrows(GatewayNotFoundException.class,
//                () -> deviceService.autoProvision(999L, 5L, "EUI-001", "센서", Set.of()));
//    }
//
//    @Test
//    @DisplayName("getDeviceById - 다른 그룹 소속이면 예외")
//    void 조회_다른그룹이면_예외() {
//        Groups group = mock(Groups.class);
//        given(group.getGroupId()).willReturn(5L);
//        Device device = Device.builder().deviceId(1L).groupId(group).build();
//
//        given(deviceRepository.findById(1L)).willReturn(Optional.of(device));
//        given(groupMembersService.validateGroupMembers(5L, 999L))
//                .willThrow(com.insighton.core.groupmember.exception.
//                        GroupMemberNotFoundException.byMemberIdAndGroupId(999L, 5L));
//
//        assertThrows(com.insighton.core.groupmember.exception.GroupMemberNotFoundException.class,
//                () -> deviceService.getDeviceById(999L, 1L));
//    }
//
//    @Test
//    @DisplayName("updateDeviceName - MEMBER 권한이면 NoPermissionException")
//    void 이름수정_권한없음() {
//        Groups group = mock(Groups.class);
//        given(group.getGroupId()).willReturn(5L);
//        Device device = Device.builder().deviceId(1L).groupId(group).build();
//
//        given(deviceRepository.findById(1L)).willReturn(Optional.of(device));
//        GroupMembers member = GroupMembers.builder().userId(1L).groupRole(GroupRole.MEMBER).build();
//        given(groupMembersService.validateGroupMembers(5L, 1L)).willReturn(member);
//
//        assertThrows(NoPermissionException.class,
//                () -> deviceService.updateDeviceName(1L, 1L, "새이름"));
//    }
//
//    @Test
//    @DisplayName("updateDeviceName - 빈 문자열이면 InvalidDeviceValueException, 리포지토리 호출 전에 걸러짐")
//    void 이름수정_빈값() {
//        assertThrows(InvalidDeviceValueException.class,
//                () -> deviceService.updateDeviceName(1L, 1L, "   "));
//        verify(deviceRepository, never()).findById(anyLong());
//    }
//
//    @Test
//    @DisplayName("deleteDevice - 없는 디바이스면 DeviceNotFoundException")
//    void 삭제_없는디바이스() {
//        given(deviceRepository.findById(999L)).willReturn(Optional.empty());
//
//        assertThrows(DeviceNotFoundException.class,
//                () -> deviceService.deleteDevice(1L, 999L));
//    }
//
//    @Test
//    @DisplayName("deleteAll - EUI가 null인 디바이스(ACTUATOR성 데이터)는 evict 대상에서 제외")
//    void 전체삭제_null_EUI_필터링() {
//        Device withEui = Device.builder().deviceId(1L).deviceEui("EUI-001").build();
//        Device withoutEui = Device.builder().deviceId(2L).deviceEui(null).build();
//
//        given(groupMembersService.validateGroupMembers(5L, 1L))
//                .willReturn(GroupMembers.builder().userId(1L).groupRole(GroupRole.MANAGER).build());
//        given(deviceRepository.findByGroupId_GroupId(5L)).willReturn(List.of(withEui, withoutEui));
//
//        deviceService.deleteAll(1L, 5L);
//
//        verify(deviceLookupCacheService, times(1)).evict("EUI-001");
//        verify(deviceAttributeRepository).deleteByGroupId_GroupId(5L);
//        verify(deviceRepository).deleteAll(List.of(withEui, withoutEui));
//    }
//}