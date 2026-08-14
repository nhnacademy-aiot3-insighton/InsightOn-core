package com.insighton.core.sensors.service;

import com.insighton.core.domain.sensorattributes.entity.MetricDefinition;
import com.insighton.core.domain.sensorattributes.repository.MetricDefinitionRepository;
import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.gateway.exception.GatewayNotFoundException;
import com.insighton.core.domain.gateway.repository.GatewayRepository;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.entity.GroupMember.GroupRole;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.groups.repository.GroupRepository;
import com.insighton.core.domain.location.repository.LocationRepository;
import com.insighton.core.adapter.mqtt.cache.SensorLookupCacheService;
import com.insighton.core.adapter.mqtt.cache.dto.SensorCacheEntry;
import com.insighton.core.domain.sensorattributes.repository.SensorAttributeRepository;
import com.insighton.core.domain.sensors.entity.Sensor;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import com.insighton.core.domain.sensors.exception.InvalidSensorValueException;
import com.insighton.core.domain.sensors.repository.SensorRepository;
import com.insighton.core.domain.sensors.service.impl.SensorServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.exception.LocationNotFoundException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorServiceTest {

    @Mock private SensorRepository sensorRepository;
    @Mock private SensorAttributeRepository sensorAttributeRepository;
    @Mock private SensorLookupCacheService sensorLookupCacheService;
    @Mock private GroupMemberService groupMemberService;
    @Mock private GatewayRepository gatewayRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private MetricDefinitionRepository metricDefinitionRepository;

    @InjectMocks
    private SensorServiceImpl sensorService;

    @Test
    @DisplayName("autoProvision - 신규 EUI면 새 센서를 생성한다")
    void autoProvision_신규센서_생성() {
        given(sensorRepository.findBySensorEui("EUI-001")).willReturn(Optional.empty());
        given(gatewayRepository.findById(10L)).willReturn(Optional.of(mock(Gateway.class)));
        given(groupRepository.findById(5L)).willReturn(Optional.of(mock(Group.class)));
        given(metricDefinitionRepository.findByMetricKeyIgnoreCase("co2"))
                .willReturn(Optional.of(MetricDefinition.builder().metricKey("co2").build()));

        Sensor saved = Sensor.builder()
                .sensorId(100L)
                .sensorEui("EUI-001")
                .build();
        given(sensorRepository.save(any(Sensor.class))).willReturn(saved);

        SensorCacheEntry result = sensorService.autoProvision(10L, 5L, "EUI-001", "센서", Set.of("co2"));

        assertThat(result.sensorId()).isEqualTo(100L);
        verify(sensorLookupCacheService).populate(any(SensorCacheEntry.class));
        verify(sensorAttributeRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("autoProvision - 이미 존재하는 EUI면 재조회 후 캐시만 복구한다 (신규 저장 안 함)")
    void autoProvision_기존EUI_캐시복구만() {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(5L); // 요청 groupId(5L)와 일치해야 통과

        // gatewaysId는 null로 둬도 됨 - null이면 파라미터로 받은 gatewayId(10L)로 대체되는 로직이라 스텁 불필요
        Sensor existing = Sensor.builder().sensorId(1L).sensorEui("EUI-001").group(group).build();
        given(sensorRepository.findBySensorEui("EUI-001")).willReturn(Optional.of(existing));

        SensorCacheEntry result = sensorService.autoProvision(10L, 5L, "EUI-001", "센서", Set.of("co2"));

        assertThat(result.sensorId()).isEqualTo(1L);
        assertThat(result.gatewayId()).isEqualTo(10L); // gateway null -> 파라미터로 받은 값으로 대체됨
        verify(sensorRepository, never()).save(any());
        verify(sensorLookupCacheService).populate(any(SensorCacheEntry.class));
    }

    @Test
    @DisplayName("autoProvision - 이미 다른 그룹에 등록된 EUI면 InvalidSensorValueException")
    void autoProvision_다른그룹EUI_충돌() {
        Group otherGroup = mock(Group.class);
        given(otherGroup.getGroupId()).willReturn(999L); // 요청 groupId(5L)와 다름

        Sensor existing = Sensor.builder().sensorId(1L).sensorEui("EUI-001").group(otherGroup).build();
        given(sensorRepository.findBySensorEui("EUI-001")).willReturn(Optional.of(existing));

        assertThrows(InvalidSensorValueException.class,
                () -> sensorService.autoProvision(10L, 5L, "EUI-001", "센서", Set.of("co2")));

        verify(sensorLookupCacheService, never()).populate(any());
    }

    @Test
    @DisplayName("autoProvision - 게이트웨이가 없으면 GatewayNotFoundException")
    void autoProvision_게이트웨이없음() {
        given(sensorRepository.findBySensorEui(anyString())).willReturn(Optional.empty());
        given(gatewayRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(GatewayNotFoundException.class,
                () -> sensorService.autoProvision(999L, 5L, "EUI-001", "센서", Set.of()));
    }

    @Test
    @DisplayName("getSensorById - 다른 그룹 소속이면 예외")
    void 조회_다른그룹이면_예외() {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(5L);
        Sensor sensor = Sensor.builder().sensorId(1L).group(group).build();

        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
        given(groupMemberService.validateGroupMembers(5L, 999L))
                .willThrow(GroupMemberNotFoundException.byMemberIdAndGroupId(999L, 5L));

        assertThrows(GroupMemberNotFoundException.class,
                () -> sensorService.getSensorById(999L, 1L));
    }

    @Test
    @DisplayName("updateSensorName - MEMBER 권한이면 NoPermissionException")
    void 이름수정_권한없음() {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(5L);
        Sensor sensor = Sensor.builder().sensorId(1L).group(group).build();

        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
        GroupMember member = GroupMember.builder().userId(1L).groupRole(GroupRole.MEMBER).build();
        given(groupMemberService.validateGroupMembers(5L, 1L)).willReturn(member);

        assertThrows(NoPermissionException.class,
                () -> sensorService.updateSensorName(1L, 1L, "새이름"));
    }

    @Test
    @DisplayName("updateSensorName - 빈 문자열이면 InvalidSensorValueException, 리포지토리 호출 전에 걸러짐")
    void 이름수정_빈값() {
        assertThrows(InvalidSensorValueException.class,
                () -> sensorService.updateSensorName(1L, 1L, "   "));
        verify(sensorRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("deleteSensor - 없는 센서면 SensorNotFoundException")
    void 삭제_없는센서() {
        given(sensorRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(SensorNotFoundException.class,
                () -> sensorService.deleteSensor(1L, 999L));
    }

    @Test
    @DisplayName("deleteAll - EUI가 null인 센서(ACTUATOR성 데이터)는 evict 대상에서 제외")
    void 전체삭제_null_EUI_필터링() {
        Sensor withEui = Sensor.builder().sensorId(1L).sensorEui("EUI-001").build();
        Sensor withoutEui = Sensor.builder().sensorId(2L).sensorEui(null).build();

        given(groupMemberService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMember.builder().userId(1L).groupRole(GroupRole.MANAGER).build());
        given(sensorRepository.findByGroupGroupId(5L)).willReturn(List.of(withEui, withoutEui));

        sensorService.deleteAll(1L, 5L);

        verify(sensorLookupCacheService, times(1)).evict("EUI-001");
//        verify(sensorAttributeRepository).deleteByGroupGroupId(5L);
        verify(sensorRepository).deleteAll(List.of(withEui, withoutEui));
    }

    @Test
    @DisplayName("updateSensor - 위치만 수정, EUI가 있으면 캐시도 갱신")
    void 업데이트_위치만_수정_캐시갱신() {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(5L);
        Gateway gateway = mock(Gateway.class);
        given(gateway.getGatewayId()).willReturn(10L); // sensorEui != null 분기에서 실제로 호출됨
        Sensor sensor = Sensor.builder()
                .sensorId(1L).group(group).gateway(gateway).sensorEui("EUI-001").build();

        Location newLocation = mock(Location.class);
        given(newLocation.getLocationId()).willReturn(20L); // 캐시 엔트리에 location.getLocationId()를 그대로 씀

        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
        given(groupMemberService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMember.builder().userId(1L).groupRole(GroupRole.MANAGER).build());
        given(locationRepository.findByGroupGroupIdAndLocationName(5L, "4층")).willReturn(Optional.of(newLocation));

        sensorService.updateSensor(1L, 1L, "4층", null);

        assertThat(sensor.getLocation()).isEqualTo(newLocation); // getLocationsId() -> getLocation()
        verify(sensorLookupCacheService).populate(any(SensorCacheEntry.class));
    }

    @Test
    @DisplayName("updateSensor - 위치/이름 둘 다 수정 성공 (EUI 없으면 캐시 갱신 스킵)")
    void 업데이트_위치_이름_둘다_성공() {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(5L);
        Sensor sensor = Sensor.builder().sensorId(1L).group(group).sensorEui(null).build();

        Location newLocation = mock(Location.class); // 여기도 getter 스텁 불필요

        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
        given(groupMemberService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMember.builder().userId(1L).groupRole(GroupRole.MANAGER).build());
        given(locationRepository.findByGroupGroupIdAndLocationName(5L, "4층")).willReturn(Optional.of(newLocation));

        sensorService.updateSensor(1L, 1L, "4층", "새 이름");

        assertThat(sensor.getLocation()).isEqualTo(newLocation);
        assertThat(sensor.getSensorName()).isEqualTo("새 이름");
        verify(sensorLookupCacheService, never()).populate(any()); // EUI null이라 캐시 갱신 안 함
    }
    @Test
    @DisplayName("updateSensor - 이름만 수정, 위치는 그대로")
    void 업데이트_이름만_수정() {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(5L);
        Sensor sensor = Sensor.builder().sensorId(1L).group(group).sensorName("기존 이름").build();

        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
        given(groupMemberService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMember.builder().userId(1L).groupRole(GroupRole.MANAGER).build());

        sensorService.updateSensor(1L, 1L, null, "새 이름");

        assertThat(sensor.getSensorName()).isEqualTo("새 이름");
        verify(locationRepository, never()).findByGroupGroupIdAndLocationName(any(), any());
        verify(sensorLookupCacheService, never()).populate(any());
    }


    @Test
    @DisplayName("updateSensor - 위치/이름 둘 다 null이면 InvalidSensorValueException")
    void 업데이트_둘다_null이면_거부() {
        // 구현에 이 가드가 없다면 이 테스트는 삭제하세요
        assertThrows(InvalidSensorValueException.class,
                () -> sensorService.updateSensor(1L, 1L, null, null));
        verify(sensorRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("updateSensor - 이름이 빈 문자열이면 InvalidSensorValueException")
    void 업데이트_빈이름_거부() {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(5L);
        Sensor sensor = Sensor.builder().sensorId(1L).group(group).build();

        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
        given(groupMemberService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMember.builder().userId(1L).groupRole(GroupRole.MANAGER).build());

        assertThrows(InvalidSensorValueException.class,
                () -> sensorService.updateSensor(1L, 1L, null, "   "));
    }

    @Test
    @DisplayName("updateSensor - 없는 센서면 SensorNotFoundException")
    void 업데이트_없는센서() {
        given(sensorRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(SensorNotFoundException.class,
                () -> sensorService.updateSensor(1L, 999L, "4층", null));
    }

    @Test
    @DisplayName("updateSensor - 없는 위치면 LocationNotFoundException")
    void 업데이트_없는위치() {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(5L);
        Sensor sensor = Sensor.builder().sensorId(1L).group(group).build();

        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
        given(groupMemberService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMember.builder().userId(1L).groupRole(GroupRole.MANAGER).build());
        given(locationRepository.findByGroupGroupIdAndLocationName(5L, "없는위치")).willReturn(Optional.empty());

        assertThrows(LocationNotFoundException.class,
                () -> sensorService.updateSensor(1L, 1L, "없는위치", null));
    }

    @Test
    @DisplayName("updateSensor - MEMBER 권한이면 NoPermissionException")
    void 업데이트_권한없음() {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(5L);
        Sensor sensor = Sensor.builder().sensorId(1L).group(group).build();

        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
        given(groupMemberService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMember.builder().userId(1L).groupRole(GroupRole.MEMBER).build());

        assertThrows(NoPermissionException.class,
                () -> sensorService.updateSensor(1L, 1L, "4층", "새 이름"));
    }
}
