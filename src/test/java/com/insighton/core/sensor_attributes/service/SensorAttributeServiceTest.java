package com.insighton.core.sensor_attributes.service;


import com.insighton.core.groupmember.entity.GroupMember;
import com.insighton.core.groupmember.entity.GroupMember.GroupRole;
import com.insighton.core.groupmember.service.GroupMemberService;
import com.insighton.core.groups.entity.Group;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.sensorattributes.entity.SensorAttribute;
import com.insighton.core.sensorattributes.repository.SensorAttributeRepository;
import com.insighton.core.sensorattributes.service.impl.SensorAttributeServiceImpl;
import com.insighton.core.sensors.entity.Sensor;
import com.insighton.core.sensors.exception.SensorNotFoundException;
import com.insighton.core.sensors.exception.InvalidSensorValueException;
import com.insighton.core.sensors.repository.SensorRepository;
import org.junit.jupiter.api.Disabled;
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

@Disabled
@ExtendWith(MockitoExtension.class)
class SensorAttributeServiceTest {

    @Mock private SensorAttributeRepository attributeRepository;
    @Mock private SensorRepository sensorRepository;
    @Mock private GroupMemberService groupMemberService;

    @InjectMocks
    private SensorAttributeServiceImpl attributeService;

    private Sensor actuatorDevice(Long groupId) {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(groupId);
        return Sensor.builder().sensorId(1L).group(group).build();
    }

    private Sensor sensorDevice(Long groupId) {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(groupId);
        return Sensor.builder().sensorId(1L).group(group).build();
    }

    // FIXME: sensor_attributes의 current_value_str 컬럼 제거로 updateActuatorValue()와
    // getCurrentValueStr()가 사라져 컴파일 불가. 액추에이터 제어가 actuators 테이블 기준으로
    // 재설계되면 다시 살릴 것.
//    void 값변경_성공() {
//        Sensor sensor = actuatorDevice(5L);
//        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
//        given(groupMemberService.validateGroupMembers(5L, 1L))
//                .willReturn(GroupMember.builder().userId(1L).groupRole(GroupRole.MANAGER).build());
//
//        SensorAttribute attribute = SensorAttribute.builder()
//                .metricKey("power_status").build();
//        given(attributeRepository.findBySensorSensorIdAndMetricKey(1L, "power_status"))
//                .willReturn(Optional.of(attribute));
//
//        attributeService.updateActuatorValue(1L, 1L, "POWER_STATUS", "ON");
//
//        assertThat(attribute.getCurrentValueStr()).isEqualTo("ON");
//    }

    // FIXME: sensor_attributes의 current_value_str 컬럼 제거로 updateActuatorValue()와
    // getCurrentValueStr()가 사라져 컴파일 불가. 액추에이터 제어가 actuators 테이블 기준으로
    // 재설계되면 다시 살릴 것.
//    void 값변경_센서타입_거부() {
//        Sensor sensor = sensorDevice(5L);
//        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
//        given(groupMemberService.validateGroupMembers(5L, 1L))
//                .willReturn(GroupMember.builder().userId(1L).groupRole(GroupRole.MANAGER).build());
//
//        assertThrows(InvalidSensorValueException.class,
//                () -> attributeService.updateActuatorValue(1L, 1L, "co2", "999"));
//    }

    // FIXME: sensor_attributes의 current_value_str 컬럼 제거로 updateActuatorValue()와
    // getCurrentValueStr()가 사라져 컴파일 불가. 액추에이터 제어가 actuators 테이블 기준으로
    // 재설계되면 다시 살릴 것.
//    void 값변경_권한없음() {
//        Sensor sensor = actuatorDevice(5L);
//        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
//        given(groupMemberService.validateGroupMembers(5L, 1L))
//                .willReturn(GroupMember.builder().userId(1L).groupRole(GroupRole.MEMBER).build());
//
//        assertThrows(NoPermissionException.class,
//                () -> attributeService.updateActuatorValue(1L, 1L, "power_status", "ON"));
//    }

    // FIXME: sensor_attributes의 current_value_str 컬럼 제거로 updateActuatorValue()와
    // getCurrentValueStr()가 사라져 컴파일 불가. 액추에이터 제어가 actuators 테이블 기준으로
    // 재설계되면 다시 살릴 것.
//    void 값변경_메트릭키없음() {
//        Sensor sensor = actuatorDevice(5L);
//        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
//        given(groupMemberService.validateGroupMembers(5L, 1L))
//                .willReturn(GroupMember.builder().userId(1L).groupRole(GroupRole.MANAGER).build());
//        given(attributeRepository.findBySensorSensorIdAndMetricKey(anyLong(), anyString()))
//                .willReturn(Optional.empty());
//
//        assertThrows(MetricKeyNotFoundException.class,
//                () -> attributeService.updateActuatorValue(1L, 1L, "power_status", "ON"));
//    }

    @Test
    @DisplayName("isValidSensorAttribute - 대소문자가 달라도 정규화되어 조회된다")
    void 유효성검증_대소문자_정규화() {
        given(attributeRepository.existsBySensorSensorIdAndMetricKey(1L, "co2")).willReturn(true);

        boolean result = attributeService.isValidSensorAttribute(1L, "CO2");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isValidSensorAttribute - 존재하지 않는 메트릭 키는 예외 없이 false")
    void 유효성검증_없는키_false() {
        boolean result = attributeService.isValidSensorAttribute(1L, "unknown_metric");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getAllAttributeBySensorId - 센서 없으면 예외")
    void 목록조회_센서없음() {
        given(sensorRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(SensorNotFoundException.class,
                () -> attributeService.getAllAttributeBySensorId(1L, 999L));
    }
}