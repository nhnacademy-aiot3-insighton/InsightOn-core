package com.insighton.core.sensors.service.impl;

import com.insighton.core.device_attributes.entity.SensorAttribute;
import com.insighton.core.device_attributes.repository.DeviceAttributeRepository;
import com.insighton.core.gateway.entity.Gateway;
import com.insighton.core.gateway.exception.GatewayNotFoundException;
import com.insighton.core.gateway.repository.GatewayRepository;
import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groupmember.service.GroupMembersService;
import com.insighton.core.groups.entity.Groups;
import com.insighton.core.groups.exception.GroupNotFoundException;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.groups.repository.GroupsRepository;
import com.insighton.core.location.entity.Locations;
import com.insighton.core.location.exception.LocationNotFoundException;
import com.insighton.core.location.repository.LocationsRepository;
import com.insighton.core.mqtt.cache.DeviceLookupCacheService;
import com.insighton.core.mqtt.cache.dto.DeviceCacheEntry;
import com.insighton.core.sensors.dto.DeviceResponse;
import com.insighton.core.sensors.entity.Device;
import com.insighton.core.sensors.entity.DeviceType;
import com.insighton.core.sensors.exception.DeviceNotFoundException;
import com.insighton.core.sensors.exception.InvalidDeviceValueException;
import com.insighton.core.sensors.repository.DeviceRepository;
import com.insighton.core.sensors.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository; // 디바이스 조회/저장
    private final DeviceAttributeRepository deviceAttributeRepository; // 디바이스 속성 조회/삭제
    private final DeviceLookupCacheService deviceLookupCacheService; // EUI 기반 캐시 계층
    private final GatewayRepository gatewayRepository; // 관계 엔티티 조회용
    private final GroupsRepository groupsRepository; // 관계 엔티티 조회용
    private final LocationsRepository locationsRepository; // 관계 엔티티 조회용
    private final GroupMembersService groupMembersService; // 그룹 멤버 권한 검증용 서비스

    /**
     * 요청자가 해당 그룹의 MANAGER 이상 권한을 가졌는지 검증하는 내부 헬퍼 메서드
     */
    private void validateManagerRole(Long userId, Long groupId) {
        GroupMembers member = groupMembersService.validateGroupMembers(groupId, userId);
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
    public DeviceCacheEntry autoProvision(
            Long gatewayId,
            Long groupId,
            String deviceEui,
            String deviceName,
            Set<String> metricKeys) {

        // 캐시 만료 시 DB 유니크 제약조건(500 에러) 충돌 방어를 위해 EUI 사전 조회
        Optional<Device> existingDevice = deviceRepository.findByDeviceEui(deviceEui);
        if (existingDevice.isPresent()) {
            Device e = existingDevice.get();
            if (!Objects.equals(e.getGroupId().getGroupId(), groupId)) {
                throw new InvalidDeviceValueException(
                        "이미 다른 그룹에 등록된 EUI입니다. (EUI: " + deviceEui + ")");
            }
            Long existingGatewayId = e.getGateway() != null ? e.getGateway().getGatewayId() : gatewayId;
            DeviceCacheEntry cacheEntry = new DeviceCacheEntry(
                    e.getDeviceId(), e.getDeviceEui(), existingGatewayId,
                    e.getLocation() != null ? e.getLocation().getLocationId() : null
            );
            deviceLookupCacheService.populate(cacheEntry); // 캐시 복구
            return cacheEntry;
        }
        // 대소문자 졍규화
        String nolDeviceName = nomalizeDeviceName(deviceName);

        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new GatewayNotFoundException("게이트웨이를 찾을 수 없습니다"));

        Groups groups = groupsRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));

        // 패킷 정보로 센서 엔티티 객체를 조립
        Device device = Device.builder()
                .deviceType(DeviceType.SENSOR) // 센서 타입으로 지정
                .gateway(gateway) // 패킷이 거쳐온 게이트웨이 ID를 입력
                .groupId(groups) // 소속 그룹아이디 주입
                .deviceEui(deviceEui) // 센서의 고유 시리얼 번호(EUI)를 입력
                .deviceName(nolDeviceName) // 패킷 정보 기반의 임시 이름(예: "Temp_Sensor_01")을 입력
                .location(null) // 설치 장소는 아직 모르므로 일단 null로 비움
                .lastSeenAt(OffsetDateTime.now()) // 첫 데이터가 도착했으니 통신 시각을 현재로 기록
                .createdAt(OffsetDateTime.now()) // 생성 시각을 현재로 저장
                .build();


        // 센서 정보를 sensor_devices DB 테이블에 저장
        Device savedDevice = deviceRepository.save(device);

        // 패킷 안에 있던 데이터 항목들(예: ["co2", "temperature"])을 확인해 속성(Attribute) 테이블도 채움
        if (metricKeys != null && !metricKeys.isEmpty()) {
            List<SensorAttribute> attributes = metricKeys.stream()
                    .map(metricKey -> SensorAttribute.builder()
                            .deviceId(savedDevice) // 방금 DB에 저장한 센서(부모)와 연결 (FK 매핑).
                            .groupId(groups) // 속성 엔티티에도 그룹 주입
                            .metricKey(metricKey) // 수집 항목 키(예: "co2")를 저장
                            .build())
                    .toList();

            // sensor_device_attributes DB 테이블에 속성들을 한 번에 저장
            deviceAttributeRepository.saveAll(attributes);
        }

        // 캐시에 올릴 경량화 객체(DeviceCacheEntry)를 생성
        DeviceCacheEntry cacheEntry = new DeviceCacheEntry(
                savedDevice.getDeviceId(), // 기기 PK 번호
                savedDevice.getDeviceEui(), // 고유 EUI
                gatewayId,
                null // 위치는 아직 없음
        );

        // 메모리 캐시(ConcurrentHashMap)에 적재하여 다음 패킷부터는 DB 조회 없이 빠르게 처리
        deviceLookupCacheService.populate(cacheEntry);

        // 생성된 캐시 엔트리를 반환
        return cacheEntry;
    }

    @Override
    public DeviceResponse getDeviceById(Long userId, Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));

        // 조회 권한 체크
        validateGroupMembership(userId, device.getGroupId().getGroupId()); // Groups 객체에서 Long ID 호출

        return toDto(device);
    }

    @Override
    @Transactional // 위치 수정 로직
    public void updateDeviceLocation(Long userId, Long deviceId, Long newLocationId) {
        if (newLocationId == null) {
            throw new InvalidDeviceValueException("변경할 위치 ID는 필수입니다.");
        }
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));

        // 쓰기 작업 권한 체크 (엔티티에 저장된 groupId 기준 - 다른 그룹 디바이스는 조작 불가)
        validateManagerRole(userId, device.getGroupId().getGroupId()); // Groups 객체에서 Long ID 호출

        Locations newLocation = locationsRepository.findById(newLocationId)
                        .orElseThrow(() -> LocationNotFoundException.notFoundLocationByLocationId(newLocationId));

        device.updateLocation(newLocation);

        // 캐시도 같이 갱신 (deviceEui가 있는 센서만 캐시에 들어있음)
        if (device.getDeviceEui() != null) {
            Long gatewayId = device.getGateway() != null ? device.getGateway().getGatewayId() : null;

            // 변경된 newLocationId를 적용하여 새로운 캐시 엔트리 생성
            DeviceCacheEntry updatedCacheEntry = new DeviceCacheEntry(
                    device.getDeviceId(),
                    device.getDeviceEui(),
                    gatewayId,
                    newLocationId
            );

            // 캐시 서비스에 최신 정보 적재
            deviceLookupCacheService.populate(updatedCacheEntry);
        }
    }

    @Override
    @Transactional // 이름 수정 로직
    public void updateDeviceName(Long userId, Long deviceId, String newDeviceName) {
        if (newDeviceName == null || newDeviceName.trim().isEmpty()) {
            throw new InvalidDeviceValueException("변경할 장치 이름은 필수입니다.");
        }
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));

        // 쓰기 작업 권한 체크
        validateManagerRole(userId, device.getGroupId().getGroupId()); // Groups 객체에서 Long ID 호출

        device.updateName(newDeviceName);
    }

    @Override
    public void updateDevice(Long userId, Long deviceId, Long newLocationId, String newDeviceName) {
        // 아무 필드도 안 왔으면 의미없는 요청으로 간주 (정책에 따라 이 체크는 빼셔도 됩니다)
        if (newLocationId == null && newDeviceName == null) {
            throw new InvalidDeviceValueException("변경할 값이 하나도 없습니다.");
        }

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));

        // 권한 체크는 한 번만 - 위치/이름 둘 다 바꿔도 검증 한 번으로 충분
        validateManagerRole(userId, device.getGroupId().getGroupId());

        if (newLocationId != null) {
            Locations location = locationsRepository.findById(newLocationId)
                    .orElseThrow(() -> LocationNotFoundException.notFoundLocationByLocationId(newLocationId));
            device.updateLocation(location);

            // 캐시도 같이 갱신 (deviceEui가 있는 센서만 캐시에 들어있음)
            if (device.getDeviceEui() != null) {
                Long gatewayId = device.getGateway() != null ? device.getGateway().getGatewayId() : null;
                DeviceCacheEntry updatedCacheEntry = new DeviceCacheEntry(
                        device.getDeviceId(), device.getDeviceEui(), gatewayId, newLocationId);
                deviceLookupCacheService.populate(updatedCacheEntry);
            }
        }

        if (newDeviceName != null) {
            if (newDeviceName.isBlank()) {
                throw new InvalidDeviceValueException("변경할 장치 이름은 빈 값일 수 없습니다.");
            }
            device.updateName(newDeviceName);
        }
    }

    @Override
    @Transactional // 센서 통신 시각 최신화 로직
    public void handlePacketReceived(String deviceEui) {
        if (deviceEui == null || deviceEui.trim().isEmpty()) {
            return;
        }
        deviceRepository.findByDeviceEui(deviceEui)
                .ifPresent(Device::updateLastSeen);
    }

    @Override
    @Transactional // 삭제 시 부모/자식 테이블 안전 삭제
    public void deleteDevice(Long userId, Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));

        // 삭제 작업 권한 체크
        validateManagerRole(userId, device.getGroupId().getGroupId()); // Groups 객체에서 Long ID 호출

        // @Query 없이 완벽하게 동작하는 자식 테이블 일괄 삭제 메서드 호출
        deviceAttributeRepository.deleteByDeviceId(deviceId);
        deviceRepository.delete(device);

        // 캐시에서도 제거
        if(device.getDeviceEui() != null){
            deviceLookupCacheService.evict(device.getDeviceEui());
        }
    }

    @Override
    @Transactional
    public void deleteAll(Long userId, Long groupId) {
        // 전체 삭제 필수 권한 체크
        validateManagerRole(userId, groupId);

        // 캐시 삭제 - groupId 소속 디바이스만 골라서 evict (clear()는 다른 그룹 캐시까지 날려버리므로 사용 금지)
        List<Device> devices = deviceRepository.findByGroupId(groupId);
        devices.stream()
                .map(Device::getDeviceEui) // 각디바이스의 EUI만 추출
                .filter(Objects::nonNull) // ACTUATOR타입은 EUI가 null이라 걸러냄
                .forEach(deviceLookupCacheService::evict); // 하나씩 evict 호출

        // DB삭제 (groupId 소속만)
        deviceAttributeRepository.deleteByGroupId(groupId);
        deviceRepository.deleteAll(devices);
    }

    @Override
    public List<DeviceResponse> searchDevices(Long userId, Long groupId, Long id, String eui,
                                              Long locationId, String deviceName) {
        // 검색은 그룸 범위 자체를 명시적으로 받아서 그 안에서만 조회
        validateGroupMembership(userId, groupId);

        List<Device> entities;

        if (id != null) {
            entities = deviceRepository.findById(id).map(List::of).orElse(List.of());
        } else if (eui != null && !eui.trim().isEmpty()) {
            entities = deviceRepository.findByDeviceEui(eui).map(List::of).orElse(List.of());
        } else if (locationId != null) {
            entities = deviceRepository.findByLocationId(locationId);
        } else if (deviceName != null && !deviceName.trim().isEmpty()) {
            entities = deviceRepository.findByDeviceName(deviceName);
        } else {
            entities = deviceRepository.findByGroupId(groupId);
        }

        // 위 분기들은 groupId로 안걸렀으니 다른 그룹 결과가 섞이지 않게 마지막에 한번더 필터링
        return entities.stream()
                .filter(e -> Objects.equals(e.getGroupId().getGroupId(), groupId))
                .map(this::toDto)
                .toList();
    }

    private DeviceResponse toDto(Device e) {
        return new DeviceResponse(
                e.getDeviceId(),
                e.getDeviceType(),
                e.getGateway() != null ? e.getGateway().getGatewayId() : null,
                e.getLocation() != null ? e.getLocation().getLocationId() : null,
                e.getDeviceEui(),
                e.getDeviceName(),
                e.getCreatedAt(),
                e.getLastSeenAt()
        );
    }

    // 대소문자 졍규화
    private String nomalizeDeviceName(String name){
        if(name == null || name.trim().isEmpty()){
            return name;
        }
        return name.trim().toUpperCase();
    }
}