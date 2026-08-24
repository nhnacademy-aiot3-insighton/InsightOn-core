package com.insighton.core.sensors.service;

import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.gateway.exception.GatewayNotFoundException;
import com.insighton.core.domain.gateway.repository.GatewayRepository;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.exception.GroupNotFoundException;
import com.insighton.core.domain.groups.repository.GroupRepository;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.exception.LocationNotFoundException;
import com.insighton.core.domain.location.repository.LocationRepository;
import com.insighton.core.adapter.mqtt.cache.SensorLookupCacheService;
import com.insighton.core.adapter.mqtt.cache.dto.SensorCacheEntry;
import com.insighton.core.domain.sensorattributes.repository.MetricDefinitionRepository;
import com.insighton.core.domain.sensorattributes.repository.SensorAttributeRepository;
import com.insighton.core.domain.sensors.dto.SensorResponse;
import com.insighton.core.domain.sensors.dto.SensorUpdateRequest;
import com.insighton.core.domain.sensors.entity.Sensor;
import com.insighton.core.domain.sensors.event.SensorCacheEvictEvent;
import com.insighton.core.domain.sensors.event.SensorCacheSyncEvent;
import com.insighton.core.domain.sensors.exception.InvalidSensorValueException;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import com.insighton.core.domain.sensors.repository.SensorRepository;
import com.insighton.core.domain.sensors.service.impl.SensorServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

// 권한 체크(그룹 멤버십/매니저 검증)는 usecase.sensor 패키지로 이동해서, 이 클래스는
// 순수 영속성 로직만 검증함 - 권한 관련 시나리오는 각 UseCase 테스트에서 다뤄야 함
@ExtendWith(MockitoExtension.class)
class SensorServiceTest {

    @Mock private SensorRepository sensorRepository;
    @Mock private SensorAttributeRepository sensorAttributeRepository;
    @Mock private SensorLookupCacheService sensorLookupCacheService;
    @Mock private GatewayRepository gatewayRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private MetricDefinitionRepository metricDefinitionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SensorServiceImpl sensorService;

    @Test
    @DisplayName("autoProvision - 신규 EUI면 새 센서를 생성한다")
    void autoProvision_신규센서_생성() {
        given(sensorRepository.findBySensorEui("EUI-001")).willReturn(Optional.empty());
        given(gatewayRepository.findById(10L)).willReturn(Optional.of(mock(Gateway.class)));
        given(groupRepository.findById(5L)).willReturn(Optional.of(mock(Group.class)));
        given(metricDefinitionRepository.findByMetricKeyIgnoreCase(anyString())).willReturn(Optional.empty());

        Sensor saved = Sensor.builder()
                .sensorId(100L)
                .sensorEui("EUI-001")
                .build();
        given(sensorRepository.save(any(Sensor.class))).willReturn(saved);

        SensorCacheEntry result = sensorService.autoProvision(10L, 5L, "EUI-001", "센서", Set.of("co2"));

        assertThat(result.sensorId()).isEqualTo(100L);
        // 신규 생성 분기는 populate를 직접 안 부르고 이벤트로 미룸 (AFTER_COMMIT 캐시 동기화)
        verify(eventPublisher).publishEvent(any(SensorCacheSyncEvent.class));
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
        // 캐시 복구 분기는 지금도 populate를 직접 호출함 (DB 쓰기가 없어 롤백 위험이 없으므로)
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
    @DisplayName("getSensorById - 없는 센서면 SensorNotFoundException")
    void 조회_없는센서() {
        given(sensorRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(SensorNotFoundException.class,
                () -> sensorService.getSensorById(999L));
    }

    @Test
    @DisplayName("getSensorById - 정상 조회")
    void 조회_성공() {
        Group group = mock(Group.class);
        Sensor sensor = Sensor.builder().sensorId(1L).group(group).sensorEui("EUI-001").sensorName("센서A").build();
        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));

        SensorResponse result = sensorService.getSensorById(1L);

        assertThat(result.sensorId()).isEqualTo(1L);
        assertThat(result.sensorEui()).isEqualTo("EUI-001");
    }

    @Test
    @DisplayName("deleteSensor - 없는 센서면 SensorNotFoundException")
    void 삭제_없는센서() {
        given(sensorRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(SensorNotFoundException.class,
                () -> sensorService.deleteSensor(999L));
    }

    @Test
    @DisplayName("deleteAll - EUI가 null인 센서(ACTUATOR성 데이터)는 evict 대상에서 제외")
    void 전체삭제_null_EUI_필터링() {
        Sensor withEui = Sensor.builder().sensorId(1L).sensorEui("EUI-001").build();
        Sensor withoutEui = Sensor.builder().sensorId(2L).sensorEui(null).build();

        given(sensorRepository.findByGroupGroupId(5L)).willReturn(List.of(withEui, withoutEui));

        sensorService.deleteAll(5L);

        verify(sensorLookupCacheService, times(1)).evict("EUI-001");
        verify(sensorRepository).deleteAll(List.of(withEui, withoutEui));
    }

    @Test
    @DisplayName("updateSensor - 위치만 수정, EUI가 있으면 캐시 갱신 이벤트 발행")
    void 업데이트_위치만_수정_캐시갱신() {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(5L);
        Gateway gateway = mock(Gateway.class);
        given(gateway.getGatewayId()).willReturn(10L);
        Sensor sensor = Sensor.builder()
                .sensorId(1L).group(group).gateway(gateway).sensorEui("EUI-001").build();

        Location newLocation = mock(Location.class);
        given(newLocation.getLocationId()).willReturn(20L);

        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
        given(locationRepository.findByLocationIdAndGroupGroupId(20L, 5L)).willReturn(Optional.of(newLocation));

        sensorService.updateSensor(1L, new SensorUpdateRequest(20L, null));

        assertThat(sensor.getLocation()).isEqualTo(newLocation);
        verify(eventPublisher).publishEvent(any(SensorCacheSyncEvent.class));
    }

    @Test
    @DisplayName("updateSensor - 위치/이름 둘 다 수정 성공 (EUI 없으면 캐시 이벤트 스킵)")
    void 업데이트_위치_이름_둘다_성공() {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(5L);
        Sensor sensor = Sensor.builder().sensorId(1L).group(group).sensorEui(null).build();

        Location newLocation = mock(Location.class);

        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
        given(locationRepository.findByLocationIdAndGroupGroupId(20L, 5L)).willReturn(Optional.of(newLocation));

        sensorService.updateSensor(1L, new SensorUpdateRequest(20L, "새 이름"));

        assertThat(sensor.getLocation()).isEqualTo(newLocation);
        assertThat(sensor.getSensorName()).isEqualTo("새 이름");
        verify(eventPublisher, never()).publishEvent(any()); // EUI null이라 캐시 이벤트 발행 안 함
    }

    @Test
    @DisplayName("updateSensor - 이름만 수정, 위치는 그대로")
    void 업데이트_이름만_수정() {
        Sensor sensor = Sensor.builder().sensorId(1L).sensorName("기존 이름").build();

        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));

        sensorService.updateSensor(1L, new SensorUpdateRequest(null, "새 이름"));

        assertThat(sensor.getSensorName()).isEqualTo("새 이름");
        verify(locationRepository, never()).findByLocationIdAndGroupGroupId(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("updateSensor - request 자체가 null이면 InvalidSensorValueException")
    void 업데이트_request_null이면_거부() {
        assertThrows(InvalidSensorValueException.class,
                () -> sensorService.updateSensor(1L, null));
        verify(sensorRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("updateSensor - 이름과 위치 둘 다 비어있으면(공백 포함) InvalidSensorValueException, 센서 조회도 안 함")
    void 업데이트_둘다빈값_거부() {
        assertThrows(InvalidSensorValueException.class,
                () -> sensorService.updateSensor(1L, new SensorUpdateRequest(null, "   ")));
        verify(sensorRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("updateSensor - 이름이 빈 문자열이면 기존 이름 그대로 유지 (위치만 반영)")
    void 업데이트_빈이름은_무시() {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(5L);
        Sensor sensor = Sensor.builder().sensorId(1L).group(group).sensorName("기존이름").build();
        Location location = mock(Location.class);

        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
        given(locationRepository.findByLocationIdAndGroupGroupId(20L, 5L)).willReturn(Optional.of(location));

        sensorService.updateSensor(1L, new SensorUpdateRequest(20L, "  "));

        assertThat(sensor.getSensorName()).isEqualTo("기존이름");
    }

    @Test
    @DisplayName("updateSensor - 위치가 null이면 기존 위치 그대로 유지 (이름만 반영)")
    void 업데이트_빈위치는_무시() {
        Group group = mock(Group.class);
        Location existingLocation = mock(Location.class);
        Sensor sensor = Sensor.builder().sensorId(1L).group(group).location(existingLocation).sensorName("기존이름").build();

        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));

        sensorService.updateSensor(1L, new SensorUpdateRequest(null, "새이름"));

        assertThat(sensor.getSensorName()).isEqualTo("새이름");
        assertThat(sensor.getLocation()).isSameAs(existingLocation);
        verify(locationRepository, never()).findByLocationIdAndGroupGroupId(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("updateSensor - 없는 센서면 SensorNotFoundException")
    void 업데이트_없는센서() {
        given(sensorRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(SensorNotFoundException.class,
                () -> sensorService.updateSensor(999L, new SensorUpdateRequest(20L, null)));
    }

    @Test
    @DisplayName("updateSensor - 없는 위치면 LocationNotFoundException")
    void 업데이트_없는위치() {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(5L);
        Sensor sensor = Sensor.builder().sensorId(1L).group(group).build();

        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
        given(locationRepository.findByLocationIdAndGroupGroupId(999L, 5L)).willReturn(Optional.empty());

        assertThrows(LocationNotFoundException.class,
                () -> sensorService.updateSensor(1L, new SensorUpdateRequest(999L, null)));
    }

    @Test
    @DisplayName("searchSensors - 조건을 리포지토리에 그대로 넘기고 결과를 DTO로 매핑해서 반환")
    void 검색_성공() {
        Group group = mock(Group.class);
        Sensor sensor = Sensor.builder().sensorId(1L).group(group).sensorEui("EUI-1").build();
        given(sensorRepository.search(5L, 1L, "EUI-1", 20L, "마루센서")).willReturn(List.of(sensor));

        List<SensorResponse> result = sensorService.searchSensors(5L, 1L, "EUI-1", new SensorUpdateRequest(20L, "마루센서"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sensorId()).isEqualTo(1L);
        verify(sensorRepository).search(5L, 1L, "EUI-1", 20L, "마루센서");
    }

    @Test
    @DisplayName("searchSensors - 조건이 전부 없어도 그대로(null) 리포지토리에 전달")
    void 검색_조건없음_그대로전달() {
        given(sensorRepository.search(5L, null, null, null, null)).willReturn(List.of());

        List<SensorResponse> result = sensorService.searchSensors(5L, null, null, new SensorUpdateRequest(null, null));

        assertThat(result).isEmpty();
        verify(sensorRepository).search(5L, null, null, null, null);
    }

    @Test
    @DisplayName("autoProvision - 그룹이 없으면 GroupNotFoundException")
    void autoProvision_그룹없음() {
        given(sensorRepository.findBySensorEui(anyString())).willReturn(Optional.empty());
        given(gatewayRepository.findById(10L)).willReturn(Optional.of(mock(Gateway.class)));
        given(groupRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(GroupNotFoundException.class,
                () -> sensorService.autoProvision(10L, 999L, "EUI-001", "센서", Set.of()));
    }

    @Test
    @DisplayName("autoProvision - metricKeys가 null이면 속성 저장을 건너뜀")
    void autoProvision_metricKeys_null이면_속성저장생략() {
        given(sensorRepository.findBySensorEui("EUI-002")).willReturn(Optional.empty());
        given(gatewayRepository.findById(10L)).willReturn(Optional.of(mock(Gateway.class)));
        given(groupRepository.findById(5L)).willReturn(Optional.of(mock(Group.class)));
        given(sensorRepository.save(any(Sensor.class)))
                .willReturn(Sensor.builder().sensorId(200L).sensorEui("EUI-002").build());

        sensorService.autoProvision(10L, 5L, "EUI-002", "센서", null);

        verify(sensorAttributeRepository, never()).saveAll(any());
        verify(metricDefinitionRepository, never()).findByMetricKeyIgnoreCase(any());
    }

    @Test
    @DisplayName("autoProvision - 이름이 공백이면 정규화하지 않고 그대로 저장")
    void autoProvision_이름공백이면_정규화안함() {
        given(sensorRepository.findBySensorEui("EUI-003")).willReturn(Optional.empty());
        given(gatewayRepository.findById(10L)).willReturn(Optional.of(mock(Gateway.class)));
        given(groupRepository.findById(5L)).willReturn(Optional.of(mock(Group.class)));
        given(sensorRepository.save(any(Sensor.class))).willAnswer(inv -> inv.getArgument(0));

        sensorService.autoProvision(10L, 5L, "EUI-003", "  ", Set.of());

        ArgumentCaptor<Sensor> captor = ArgumentCaptor.forClass(Sensor.class);
        verify(sensorRepository).save(captor.capture());
        assertThat(captor.getValue().getSensorName()).isEqualTo("  ");
    }

    @Test
    @DisplayName("detachLocationFromSensors - EUI 있는 센서는 위치 해제 + 캐시무효화 이벤트 발행")
    void 위치일괄해제_EUI있음_이벤트발행() {
        Sensor sensor = Sensor.builder().sensorId(1L).sensorEui("EUI-001").location(mock(Location.class)).build();
        given(sensorRepository.findByGroupGroupIdAndLocationLocationId(5L, 20L)).willReturn(List.of(sensor));

        sensorService.detachLocationFromSensors(5L, 20L);

        assertThat(sensor.getLocation()).isNull();
        verify(eventPublisher).publishEvent(any(SensorCacheEvictEvent.class));
    }

    @Test
    @DisplayName("detachLocationFromSensors - EUI 없는 센서는 이벤트 발행 생략")
    void 위치일괄해제_EUI없음_이벤트생략() {
        Sensor sensor = Sensor.builder().sensorId(2L).sensorEui(null).location(mock(Location.class)).build();
        given(sensorRepository.findByGroupGroupIdAndLocationLocationId(5L, 20L)).willReturn(List.of(sensor));

        sensorService.detachLocationFromSensors(5L, 20L);

        assertThat(sensor.getLocation()).isNull();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("deleteSensor - 정상 삭제, EUI 있으면 캐시도 evict")
    void 삭제_성공_EUI있음() {
        Sensor sensor = Sensor.builder().sensorId(1L).sensorEui("EUI-001").build();
        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));

        sensorService.deleteSensor(1L);

        verify(sensorAttributeRepository).deleteBySensorSensorId(1L);
        verify(sensorRepository).delete(sensor);
        verify(sensorLookupCacheService).evict("EUI-001");
    }

    @Test
    @DisplayName("deleteSensor - EUI 없으면 캐시 evict 생략")
    void 삭제_성공_EUI없음() {
        Sensor sensor = Sensor.builder().sensorId(2L).sensorEui(null).build();
        given(sensorRepository.findById(2L)).willReturn(Optional.of(sensor));

        sensorService.deleteSensor(2L);

        verify(sensorAttributeRepository).deleteBySensorSensorId(2L);
        verify(sensorRepository).delete(sensor);
        verify(sensorLookupCacheService, never()).evict(any());
    }

    @Test
    @DisplayName("getUnassignedSensors - 위치 미배정 센서 목록을 DTO로 변환해서 반환")
    void 장소미배정_조회_성공() {
        Group group = mock(Group.class);
        Sensor sensor = Sensor.builder().sensorId(1L).group(group).sensorEui("EUI-001").build();
        given(sensorRepository.findByGroupGroupIdAndLocationIsNull(5L)).willReturn(List.of(sensor));

        List<SensorResponse> result = sensorService.getUnassignedSensors(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sensorId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getSensorGroupId - 소속 그룹 ID 반환")
    void 그룹ID조회_성공() {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(5L);
        Sensor sensor = Sensor.builder().sensorId(1L).group(group).build();
        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));

        assertThat(sensorService.getSensorGroupId(1L)).isEqualTo(5L);
    }

    @Test
    @DisplayName("getSensorGroupId - 없는 센서면 SensorNotFoundException")
    void 그룹ID조회_없는센서() {
        given(sensorRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(SensorNotFoundException.class, () -> sensorService.getSensorGroupId(999L));
    }
}
