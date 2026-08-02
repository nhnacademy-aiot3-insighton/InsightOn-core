package com.insighton.core.device_attributes.service;

import com.insighton.core.device_attributes.entity.DeviceAttributeEntity;
import com.insighton.core.device_attributes.exception.MetricKeyNotFoundException;
import com.insighton.core.device_attributes.repository.DeviceAttributeRepository;
import com.insighton.core.device_attributes.service.impl.DeviceAttributeServiceImpl;
import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groupmember.entity.GroupMembers.GroupRole;
import com.insighton.core.groupmember.service.GroupMembersService;
import com.insighton.core.groups.entity.Groups;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.sensors.entity.DeviceEntity;
import com.insighton.core.sensors.entity.DeviceType;
import com.insighton.core.sensors.exception.DeviceNotFoundException;
import com.insighton.core.sensors.exception.InvalidDeviceValueException;
import com.insighton.core.sensors.repository.DeviceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DeviceAttributeServiceTest {

    @Mock private DeviceAttributeRepository attributeRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private GroupMembersService groupMembersService;

    @InjectMocks
    private DeviceAttributeServiceImpl attributeService;

    private DeviceEntity actuatorDevice(Long groupId) {
        Groups group = mock(Groups.class);
        given(group.getGroupId()).willReturn(groupId);
        return DeviceEntity.builder().deviceId(1L).deviceType(DeviceType.ACTUATOR).groupId(group).build();
    }

    private DeviceEntity sensorDevice(Long groupId) {
        Groups group = mock(Groups.class);
        given(group.getGroupId()).willReturn(groupId);
        return DeviceEntity.builder().deviceId(1L).deviceType(DeviceType.SENSOR).groupId(group).build();
    }

    @Test
    @DisplayName("updateActuatorValue - 정상 케이스")
    void 값변경_성공() {
        DeviceEntity device = actuatorDevice(5L);
        given(deviceRepository.findById(1L)).willReturn(Optional.of(device));
        given(groupMembersService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMembers.builder().userId(1L).groupRole(GroupRole.MANAGER).build());

        DeviceAttributeEntity attribute = DeviceAttributeEntity.builder()
                .metricKey("power_status").build();
        given(attributeRepository.findByDeviceId_DeviceIdAndMetricKey(1L, "power_status"))
                .willReturn(Optional.of(attribute));

        attributeService.updateActuatorValue(1L, 1L, "POWER_STATUS", "ON");

        assertThat(attribute.getCurrentValueStr()).isEqualTo("ON");
    }

    @Test
    @DisplayName("updateActuatorValue - SENSOR 타입은 제어 API로 수정 불가")
    void 값변경_센서타입_거부() {
        DeviceEntity device = sensorDevice(5L);
        given(deviceRepository.findById(1L)).willReturn(Optional.of(device));
        given(groupMembersService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMembers.builder().userId(1L).groupRole(GroupRole.MANAGER).build());

        assertThrows(InvalidDeviceValueException.class,
                () -> attributeService.updateActuatorValue(1L, 1L, "co2", "999"));
    }

    @Test
    @DisplayName("updateActuatorValue - MEMBER 권한이면 거부")
    void 값변경_권한없음() {
        DeviceEntity device = actuatorDevice(5L);
        given(deviceRepository.findById(1L)).willReturn(Optional.of(device));
        given(groupMembersService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMembers.builder().userId(1L).groupRole(GroupRole.MEMBER).build());

        assertThrows(NoPermissionException.class,
                () -> attributeService.updateActuatorValue(1L, 1L, "power_status", "ON"));
    }

    @Test
    @DisplayName("updateActuatorValue - 등록되지 않은 메트릭 키")
    void 값변경_메트릭키없음() {
        DeviceEntity device = actuatorDevice(5L);
        given(deviceRepository.findById(1L)).willReturn(Optional.of(device));
        given(groupMembersService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMembers.builder().userId(1L).groupRole(GroupRole.MANAGER).build());
        given(attributeRepository.findByDeviceId_DeviceIdAndMetricKey(anyLong(), anyString()))
                .willReturn(Optional.empty());

        assertThrows(MetricKeyNotFoundException.class,
                () -> attributeService.updateActuatorValue(1L, 1L, "power_status", "ON"));
    }

    @Test
    @DisplayName("isValidDeviceAttribute - 대소문자가 달라도 정규화되어 조회된다")
    void 유효성검증_대소문자_정규화() {
        given(attributeRepository.existsByDeviceId_DeviceIdAndMetricKey(1L, "co2")).willReturn(true);

        boolean result = attributeService.isValidDeviceAttribute(1L, "CO2");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isValidDeviceAttribute - 존재하지 않는 메트릭 키는 예외 없이 false")
    void 유효성검증_없는키_false() {
        boolean result = attributeService.isValidDeviceAttribute(1L, "unknown_metric");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getAllAttributeByDeviceId - 디바이스 없으면 예외")
    void 목록조회_디바이스없음() {
        given(deviceRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(DeviceNotFoundException.class,
                () -> attributeService.getAllAttributeByDeviceId(1L, 999L));
    }
}