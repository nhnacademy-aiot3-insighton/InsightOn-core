package com.insighton.core.domain.sensors.service.impl;

import com.insighton.core.domain.sensorattributes.entity.MetricDefinition;
import com.insighton.core.domain.sensorattributes.entity.SensorAttribute;
import com.insighton.core.domain.sensorattributes.repository.MetricDefinitionRepository;
import com.insighton.core.domain.sensorattributes.repository.SensorAttributeRepository;
import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.gateway.exception.GatewayNotFoundException;
import com.insighton.core.domain.gateway.repository.GatewayRepository;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.exception.GroupNotFoundException;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.groups.repository.GroupRepository;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.exception.LocationNotFoundException;
import com.insighton.core.domain.location.repository.LocationRepository;
import com.insighton.core.adapter.mqtt.cache.SensorLookupCacheService;
import com.insighton.core.adapter.mqtt.cache.dto.SensorCacheEntry;
import com.insighton.core.domain.sensors.dto.SensorResponse;
import com.insighton.core.domain.sensors.entity.Sensor;
import com.insighton.core.domain.sensors.event.SensorCacheSyncEvent;
import com.insighton.core.domain.sensors.exception.InvalidSensorValueException;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import com.insighton.core.domain.sensors.repository.SensorRepository;
import com.insighton.core.domain.sensors.service.SensorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorServiceImpl implements SensorService {

    private final SensorRepository sensorRepository; // 센서 조회/저장
    private final SensorAttributeRepository sensorAttributeRepository; // 센서 속성 조회/삭제
    private final SensorLookupCacheService sensorLookupCacheService; // EUI 기반 캐시 계층
    private final GatewayRepository gatewayRepository; // 관계 엔티티 조회용
    private final GroupRepository groupsRepository; // 관계 엔티티 조회용
    private final LocationRepository locationsRepository; // 관계 엔티티 조회용
    private final GroupMemberService groupMembersService; // 그룹 멤버 권한 검증용 서비스
    private final MetricDefinitionRepository metricDefinitionRepository; // 기기속성쪽 메트릭키를 메트릭정의 메트릭키값 주입
    private final ApplicationEventPublisher eventPublisher; // 캐시 갱신을 트랜잭션 커밋 후로 미루기 위한 이벤트 발행용

    /**
     * 요청자가 해당 그룹의 MANAGER 이상 권한을 가졌는지 검증하는 내부 헬퍼 메서드
     * 유스케이스 처리해서 리팩토링해보기 순환참조 문제 해결을 위해서
     */
    private void validateManagerRole(Long userId, Long groupId) {
        GroupMember member = groupMembersService.validateGroupMembers(groupId, userId);
        if (member.isMember()) {
            throw NoPermissionException.forAdmin(member.getGroupMemberId());
        }
    }

    // 조회용 그룹 소속만 환인, 역할은 무관 그룹내 소속인원만 확인 가능하게
    private void validateGroupMembership(Long userId, Long groupId) {
        groupMembersService.validateGroupMembers(groupId, userId);
    }

    @Override
    @Transactional
    public SensorCacheEntry autoProvision(
            Long gatewayId,
            Long groupId,
            String sensorEui,
            String sensorName,
            Set<String> metricKeys) {

        // 캐시 만료 시 DB 유니크 제약조건(500 에러) 충돌 방어를 위해 EUI 사전 조회
        Optional<Sensor> existingSensor = sensorRepository.findBySensorEui(sensorEui);
        if (existingSensor.isPresent()) {
            Sensor e = existingSensor.get();
            if (!Objects.equals(e.getGroup().getGroupId(), groupId)) {
                throw new InvalidSensorValueException(
                        "이미 다른 그룹에 등록된 EUI입니다. (EUI: " + sensorEui + ")");
            }
            Long existingGatewayId = e.getGateway() != null ? e.getGateway().getGatewayId() : gatewayId;
            SensorCacheEntry cacheEntry = new SensorCacheEntry(
                    e.getSensorId(), e.getSensorEui(), existingGatewayId,
                    e.getLocation() != null ? e.getLocation().getLocationId() : null
            );
            sensorLookupCacheService.populate(cacheEntry); // 캐시 복구
            return cacheEntry;
        }
        // 대소문자 졍규화
        String nolSensorName = nomalizeSensorName(sensorName);

        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new GatewayNotFoundException("게이트웨이를 찾을 수 없습니다"));

        Group groups = groupsRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));

        // 패킷 정보로 센서 엔티티 객체를 조립
        Sensor sensor = Sensor.builder()
                .gateway(gateway) // 패킷이 거쳐온 게이트웨이 ID를 입력
                .group(groups) // 소속 그룹아이디 주입
                .sensorEui(sensorEui) // 센서의 고유 시리얼 번호(EUI)를 입력
                .sensorName(nolSensorName) // 패킷 정보 기반의 임시 이름(예: "Temp_Sensor_01")을 입력
                .location(null) // 설치 장소는 아직 모르므로 일단 null로 비움
                .lastSeenAt(OffsetDateTime.now()) // 첫 데이터가 도착했으니 통신 시각을 현재로 기록
                .createdAt(OffsetDateTime.now()) // 생성 시각을 현재로 저장
                .build();


        // 센서 정보를 sensors DB 테이블에 저장
        Sensor savedSensor = sensorRepository.save(sensor);

        // 패킷 안에 있던 데이터 항목들(예: ["co2", "temperature"])을 확인해 속성(Attribute) 테이블도 채움
        if (metricKeys != null && !metricKeys.isEmpty()) {
            List<SensorAttribute> attributes = metricKeys.stream()
                    .map(metricDefinitionRepository::findByMetricKeyIgnoreCase) // 각 키를 실제 정의로 조회 (대소문자 무시)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(MetricDefinition::getMetricKey) // 정규화된(canonical) 키로 통일 - 패킷 원본 대소문자를 그대로 안 씀
                    .distinct() // 패킷에 "co2"와 "CO2"가 같이 왔어도 정규화 후엔 같은 값이라 중복 제거 (유니크 제약 위반 방지)
                    .map(normalizedKey -> SensorAttribute.builder()
                            .sensor(savedSensor)
                            .metricKey(normalizedKey)
                            .build())
                    .toList();

            if (attributes.size() < metricKeys.size()) {
                log.warn("등록되지 않은 메트릭키가 패킷에 있음 sensorEui={}, 전체={}, 저장={}건",
                        sensorEui, metricKeys.size(), attributes.size());
            }

            sensorAttributeRepository.saveAll(attributes);
        }

        // 캐시에 올릴 경량화 객체(SensorCacheEntry)를 생성
        SensorCacheEntry cacheEntry = new SensorCacheEntry(
                savedSensor.getSensorId(), // 기기 PK 번호
                savedSensor.getSensorEui(), // 고유 EUI
                gatewayId,
                null // 위치는 아직 없음
        );

        // 메모리 캐시(ConcurrentHashMap)에 적재하여 다음 패킷부터는 DB 조회 없이 빠르게 처리
        // (이 호출 뒤에 실패할 수 있는 로직이 없어 롤백 시 정합성 깨질 위험이 없으므로 이벤트로 미룰 필요 없음)
        sensorLookupCacheService.populate(cacheEntry);

        // 생성된 캐시 엔트리를 반환
        return cacheEntry;
    }

    @Override
    public SensorResponse getSensorById(Long userId, Long sensorId) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new SensorNotFoundException(sensorId));

        // 조회 권한 체크
        validateGroupMembership(userId, sensor.getGroup().getGroupId()); // Groups 객체에서 Long ID 호출

        return toDto(sensor);
    }

    @Override
    @Transactional // 위치 수정 로직
    public void updateSensorLocation(Long userId, Long sensorId, Long newLocationId) {
        if (newLocationId == null) {
            throw new InvalidSensorValueException("변경할 위치 ID는 필수입니다.");
        }
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new SensorNotFoundException(sensorId));

        // 쓰기 작업 권한 체크 (엔티티에 저장된 groupId 기준 - 다른 그룹 센서는 조작 불가)
        validateManagerRole(userId, sensor.getGroup().getGroupId()); // Groups 객체에서 Long ID 호출

        Location newLocation = locationsRepository.findById(newLocationId)
                .orElseThrow(() -> LocationNotFoundException.notFoundLocationByLocationId(newLocationId));

        sensor.updateLocation(newLocation);

        // 캐시도 같이 갱신 (sensorEui가 있는 센서만 캐시에 들어있음)
        // populate를 여기서 바로 부르지 않고 이벤트만 발행함 - SensorCacheEventListener가
        // 트랜잭션 커밋 후에만 실제로 캐시를 갱신하므로, 이 메서드 뒤쪽에서 예외가 나 롤백돼도
        // 캐시가 먼저 바뀐 값을 들고 있는 정합성 문제가 생기지 않음
        if (sensor.getSensorEui() != null) {
            Long gatewayId = sensor.getGateway() != null ? sensor.getGateway().getGatewayId() : null;

            // 변경된 newLocationId를 적용하여 새로운 캐시 엔트리 생성
            SensorCacheEntry updatedCacheEntry = new SensorCacheEntry(
                    sensor.getSensorId(),
                    sensor.getSensorEui(),
                    gatewayId,
                    newLocationId
            );

            eventPublisher.publishEvent(new SensorCacheSyncEvent(updatedCacheEntry));
        }
    }

    @Override
    @Transactional
    public void detachLocationFromSensors(Long groupId, Long locationId) {
        List<Sensor> sensors = sensorRepository.findByGroupGroupIdAndLocationLocationId(groupId, locationId);
        for (Sensor sensor : sensors) {
            sensor.updateLocation(null);

            sensorLookupCacheService.evict(sensor.getSensorEui());
        }
    }

    @Override
    @Transactional // 이름 수정 로직
    public void updateSensorName(Long userId, Long sensorId, String newSensorName) {
        if (newSensorName == null || newSensorName.trim().isEmpty()) {
            throw new InvalidSensorValueException("변경할 장치 이름은 필수입니다.");
        }
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new SensorNotFoundException(sensorId));

        // 쓰기 작업 권한 체크
        validateManagerRole(userId, sensor.getGroup().getGroupId()); // Groups 객체에서 Long ID 호출

        sensor.updateName(newSensorName);
    }

    @Override
    @Transactional
    public void updateSensor(Long userId, Long sensorId, String newLocationName, String newSensorName) {
        // 아무 필드도 안 왔으면 의미없는 요청으로 간주 (정책에 따라 이 체크는 빼셔도 됩니다)
        if (newLocationName == null && newSensorName == null) {
            throw new InvalidSensorValueException("변경할 값이 없습니다.");
        }

        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new SensorNotFoundException(sensorId));

        // 권한 체크는 한 번만 - 위치/이름 둘 다 바꿔도 검증 한 번으로 충분
        validateManagerRole(userId, sensor.getGroup().getGroupId());

        if (newLocationName != null) {
            // 사용자는 locationId를 모르므로 센서가 속한 그룹 내에서 이름으로 찾음
            // (그룹 스코프로 찾기 때문에 다른 그룹 소속 location으로 잘못 옮겨질 걱정도 없음)
            Location location = locationsRepository.findByGroupGroupIdAndLocationName(
                            sensor.getGroup().getGroupId(), newLocationName)
                    .orElseThrow(() -> LocationNotFoundException.notFoundLocationByName(newLocationName));
            sensor.updateLocation(location);

            // 캐시도 같이 갱신 (sensorEui가 있는 센서만 캐시에 들어있음)
            // populate를 바로 부르지 않고 이벤트만 발행 - 아래 이름 검증에서 예외가 나 트랜잭션이
            // 롤백되면 이 이벤트 자체가 소비되지 않아서(SensorCacheEventListener가 AFTER_COMMIT),
            // "DB는 롤백됐는데 캐시엔 바뀐 위치가 남아있는" 정합성 문제가 생기지 않음
            if (sensor.getSensorEui() != null) {
                Long gatewayId = sensor.getGateway() != null ? sensor.getGateway().getGatewayId() : null;
                SensorCacheEntry updatedCacheEntry = new SensorCacheEntry(
                        sensor.getSensorId(), sensor.getSensorEui(), gatewayId, location.getLocationId());
                eventPublisher.publishEvent(new SensorCacheSyncEvent(updatedCacheEntry));
            }
        }

        if (newSensorName != null) {
            if (newSensorName.isBlank()) {
                throw new InvalidSensorValueException("변경할 장치 이름은 빈 값일 수 없습니다.");
            }
            sensor.updateName(newSensorName);
        }
    }

    @Override
    @Transactional // 센서 통신 시각 최신화 로직
    public void handlePacketReceived(String sensorEui) {
        if (sensorEui == null || sensorEui.trim().isEmpty()) {
            return;
        }
        sensorRepository.findBySensorEui(sensorEui)
                .ifPresent(Sensor::updateLastSeen);
    }

    @Override
    @Transactional // 삭제 시 부모/자식 테이블 안전 삭제
    public void deleteSensor(Long userId, Long sensorId) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new SensorNotFoundException(sensorId));

        // 삭제 작업 권한 체크
        validateManagerRole(userId, sensor.getGroup().getGroupId()); // Groups 객체에서 Long ID 호출

        // @Query 없이 완벽하게 동작하는 자식 테이블 일괄 삭제 메서드 호출
        sensorAttributeRepository.deleteBySensorSensorId(sensorId);
        sensorRepository.delete(sensor);

        // 캐시에서도 제거
        if (sensor.getSensorEui() != null) {
            sensorLookupCacheService.evict(sensor.getSensorEui());
        }
    }

    @Override
    @Transactional
    public void deleteAll(Long userId, Long groupId) {
        // 전체 삭제 필수 권한 체크
        validateManagerRole(userId, groupId);

        // 캐시 삭제 - groupId 소속 센서만 골라서 evict (clear()는 다른 그룹 캐시까지 날려버리므로 사용 금지)
        List<Sensor> sensors = sensorRepository.findByGroupGroupId(groupId);
        sensors.stream()
                .map(Sensor::getSensorEui) // 각센서의 EUI만 추출
                .filter(Objects::nonNull) // ACTUATOR타입은 EUI가 null이라 걸러냄
                .forEach(sensorLookupCacheService::evict); // 하나씩 evict 호출

        // DB삭제 (groupId 소속만) 기기속성쪽에서 group이 빠져서 코드 재조정
        List<Long> sensorIds = sensors.stream().map(Sensor::getSensorId).toList();
        sensorAttributeRepository.deleteAllBySensorSensorIdIn(sensorIds);
        sensorRepository.deleteAll(sensors);
    }

    @Override
    public List<SensorResponse> searchSensors(Long userId, Long groupId, Long id, String eui,
                                              Long locationId, String sensorName) {
        // 검색은 그룸 범위 자체를 명시적으로 받아서 그 안에서만 조회
        validateGroupMembership(userId, groupId);

        List<Sensor> entities;

        if (id != null) {
            entities = sensorRepository.findById(id).map(List::of).orElse(List.of());
        } else if (eui != null && !eui.trim().isEmpty()) {
            entities = sensorRepository.findBySensorEui(eui).map(List::of).orElse(List.of());
        } else if (locationId != null) {
            entities = sensorRepository.findByLocationLocationId(locationId);
        } else if (sensorName != null && !sensorName.trim().isEmpty()) {
            entities = sensorRepository.findBySensorName(sensorName);
        } else {
            entities = sensorRepository.findByGroupGroupId(groupId);
        }

        // 위 분기들은 groupId로 안걸렀으니 다른 그룹 결과가 섞이지 않게 마지막에 한번더 필터링
        return entities.stream()
                .filter(e -> Objects.equals(e.getGroup().getGroupId(), groupId))
                .map(this::toDto)
                .toList();
    }

    private SensorResponse toDto(Sensor e) {
        return new SensorResponse(
                e.getSensorId(),
                e.getGateway() != null ? e.getGateway().getGatewayId() : null,
                e.getLocation() != null ? e.getLocation().getLocationId() : null,
                e.getSensorEui(),
                e.getSensorName(),
                e.getCreatedAt(),
                e.getLastSeenAt()
        );
    }

    // 대소문자 졍규화
    private String nomalizeSensorName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }
        return name.trim().toUpperCase();
    }
}
