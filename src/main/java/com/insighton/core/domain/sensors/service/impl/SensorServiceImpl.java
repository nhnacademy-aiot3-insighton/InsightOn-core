package com.insighton.core.domain.sensors.service.impl;

import com.insighton.core.adapter.mqtt.cache.SensorLookupCacheService;
import com.insighton.core.adapter.mqtt.cache.dto.SensorCacheEntry;
import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.gateway.exception.GatewayNotFoundException;
import com.insighton.core.domain.gateway.repository.GatewayRepository;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.exception.GroupNotFoundException;
import com.insighton.core.domain.groups.repository.GroupRepository;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.exception.LocationNotFoundException;
import com.insighton.core.domain.location.repository.LocationRepository;
import com.insighton.core.domain.sensorattributes.entity.MetricDefinition;
import com.insighton.core.domain.sensorattributes.entity.SensorAttribute;
import com.insighton.core.domain.sensorattributes.repository.MetricDefinitionRepository;
import com.insighton.core.domain.sensorattributes.repository.SensorAttributeRepository;
import com.insighton.core.domain.sensors.dto.SensorResponse;
import com.insighton.core.domain.sensors.dto.SensorUpdateRequest;
import com.insighton.core.domain.sensors.entity.QSensor;
import com.insighton.core.domain.sensors.entity.Sensor;
import com.insighton.core.domain.sensors.event.SensorCacheEvictEvent;
import com.insighton.core.domain.sensors.event.SensorCacheSyncEvent;
import com.insighton.core.domain.sensors.exception.InvalidSensorValueException;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import com.insighton.core.domain.sensors.repository.SensorRepository;
import com.insighton.core.domain.sensors.service.SensorService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

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
    private final MetricDefinitionRepository metricDefinitionRepository; // 기기속성쪽 메트릭키를 메트릭정의 메트릭키값 주입
    private final ApplicationEventPublisher eventPublisher; // 캐시 갱신을 트랜잭션 커밋 후로 미루기 위한 이벤트 발행용

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
                .createdAt(OffsetDateTime.now()) // 생성 시각을 현재로 저장
                .build();


        // 센서 정보를 sensors DB 테이블에 저장
        // 위 사전 체크(findBySensorEui)는 캐시 만료 후 재조회 케이스만 방어할 뿐, 체크~save() 사이에
        // 끼어드는 진짜 동시 요청은 못 막음 - 그 경우 유니크 제약 위반(DataIntegrityViolationException)을
        // 직접 잡아서, 먼저 저장된 쪽을 조회해 쓰는 것으로 복구
        Sensor savedSensor;
        try {
            savedSensor = sensorRepository.save(sensor);
        } catch (DataIntegrityViolationException e) {
            savedSensor = sensorRepository.findBySensorEui(sensorEui)
                    .orElseThrow(() -> e);
        }
        final Sensor finalSavedSensor = savedSensor; // 람다에서 캡처하려면 effectively final 필요 (try/catch 양쪽에서 대입돼서 그대로는 안 됨)

        // 패킷 안에 있던 데이터 항목들(예: ["co2", "temperature"])을 확인해 속성(Attribute) 테이블도 채움
        if (metricKeys != null && !metricKeys.isEmpty()) {
            List<SensorAttribute> attributes = metricKeys.stream()
                    .map(metricDefinitionRepository::findByMetricKeyIgnoreCase) // 각 키를 실제 정의로 조회 (대소문자 무시)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(MetricDefinition::getMetricKey) // 정규화된(canonical) 키로 통일 - 패킷 원본 대소문자를 그대로 안 씀
                    .distinct() // 패킷에 "co2"와 "CO2"가 같이 왔어도 정규화 후엔 같은 값이라 중복 제거 (유니크 제약 위반 방지)
                    .map(normalizedKey -> SensorAttribute.builder()
                            .sensor(finalSavedSensor)
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

        // 캐시 갱신은 이벤트로 미뤄서 트랜잭션 커밋 후에만 반영 (SensorCacheEventListener가 AFTER_COMMIT에 처리)
        // - 이 메서드 본문에 이후 로직이 없어도, flush/commit 자체는 메서드 리턴 이후(트랜잭션 종료 시점)에 일어나므로
        //   여기서 바로 populate하면 flush 실패 시 DB는 롤백되는데 캐시엔 값이 남는 정합성 문제가 생길 수 있음
        eventPublisher.publishEvent(new SensorCacheSyncEvent(cacheEntry));

        // 생성된 캐시 엔트리를 반환
        return cacheEntry;
    }

    @Override
    public SensorResponse getSensorById(Long sensorId) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new SensorNotFoundException(sensorId));

        return toDto(sensor);
    }

    @Override
    @Transactional
    public void detachLocationFromSensors(Long groupId, Long locationId) {
        List<Sensor> sensors = sensorRepository.findByGroupGroupIdAndLocationLocationId(groupId, locationId);
        for (Sensor sensor : sensors) {
            sensor.updateLocation(null);

            // 캐시 무효화는 바로 안 하고 이벤트만 발행 - 이 뒤로 액추에이터/대시보드/위치 삭제가 더 남아있어서
            // 그 중 하나라도 실패해 트랜잭션이 롤백되면, 커밋 전에 이미 evict된 캐시만 남는 정합성 문제가 생길 수 있음
            if (sensor.getSensorEui() != null) {
                eventPublisher.publishEvent(new SensorCacheEvictEvent(sensor.getSensorEui()));
            }
        }
    }


    @Override
    @Transactional
    public void updateSensor(Long sensorId, SensorUpdateRequest request) {
        if (request == null) {
            throw new InvalidSensorValueException("변경할 값이 없습니다.");
        }

        boolean hasLocationId = request.locationId() != null;
        boolean hasSensorName = request.sensorName() != null && !request.sensorName().isBlank();

        // 둘 다 비어있으면(null 또는 빈 문자열) 의미없는 요청으로 간주 - 하나만 비어있으면
        // 그 필드는 기존 값 유지로 취급하고 나머지 필드만 반영함
        if (!hasLocationId && !hasSensorName) {
            throw new InvalidSensorValueException("변경할 값이 없습니다.");
        }

        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new SensorNotFoundException(sensorId));


        if (hasLocationId) {
            // 센서가 속한 그룹 소속 location인지 함께 확인 (다른 그룹 소속 location으로 잘못 옮겨질 걱정도 없음)
            Location location = locationsRepository.findByLocationIdAndGroupGroupId(
                            request.locationId(), sensor.getGroup().getGroupId())
                    .orElseThrow(() -> LocationNotFoundException.notFoundLocationByLocationId(request.locationId()));
            // 그 정소를 가지고 센서는 업데이트함
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

        if (hasSensorName) {
            sensor.updateName(request.sensorName());
        }
    }

    @Override
    @Transactional // 삭제 시 부모/자식 테이블 안전 삭제
    public void deleteSensor(Long sensorId) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new SensorNotFoundException(sensorId));


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
    public void deleteAll(Long groupId) {

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
    public List<SensorResponse> searchSensors(Long groupId, String eui, SensorUpdateRequest request) {

        QSensor sensor = QSensor.sensor;
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(sensor.group.groupId.eq(groupId)); // 항상 그룹 스코프로 제한

        // eui/locationId/sensorName 중 값이 있는 조건만 AND로 조합 (없는 조건은 건너뜀)
        if (eui != null && !eui.trim().isEmpty()) {
            builder.and(sensor.sensorEui.eq(eui));
        }
        if (request.locationId() != null) {
            builder.and(sensor.location.locationId.eq(request.locationId()));
        }
        if (request.sensorName() != null && !request.sensorName().trim().isEmpty()) {
            builder.and(sensor.sensorName.eq(request.sensorName()));
        }

        return StreamSupport.stream(sensorRepository.findAll(builder).spliterator(), false)
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<SensorResponse> getUnassignedSensors(Long groupId) {
        return sensorRepository.findByGroupGroupIdAndLocationIsNull(groupId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public Long getSensorGroupId(Long sensorId) {
        return sensorRepository.findById(sensorId)
                .orElseThrow(() -> new SensorNotFoundException(sensorId))
                .getGroup().getGroupId();
    }

    private SensorResponse toDto(Sensor e) {
        return new SensorResponse(
                e.getSensorId(),
                e.getGateway() != null ? e.getGateway().getGatewayId() : null,
                e.getLocation() != null ? e.getLocation().getLocationId() : null,
                e.getSensorEui(),
                e.getSensorName(),
                e.getCreatedAt()
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
