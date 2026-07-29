package com.insighton.core.sensors.service.impl;

import com.insighton.core.device_attributes.entity.DeviceAttributeEntity;
import com.insighton.core.device_attributes.repository.DeviceAttributeRepository;
import com.insighton.core.sensors.dto.DeviceResponse;
import com.insighton.core.sensors.entity.DeviceCacheEntry;
import com.insighton.core.sensors.entity.DeviceEntity;
import com.insighton.core.sensors.entity.DeviceType;
import com.insighton.core.sensors.repository.DeviceRepository;
import com.insighton.core.sensors.service.DeviceLookupCacheService;
import com.insighton.core.sensors.service.DeviceService;
import com.insighton.core.exception.CustomException;
import com.insighton.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceAttributeRepository deviceAttributeRepository;
    private final DeviceLookupCacheService deviceLookupCacheService;

    @Override
    @Transactional
    public DeviceCacheEntry autoProvision(Long gatewayId, Long groupId, String deviceEui, String deviceName, Set<String> metricKeys) {

        // 대소문자 졍규화
        String nolDeviceName = nomalizeDeviceName(deviceName);

        // [1단계] 패킷 정보로 센서 엔티티 객체를 조립
        DeviceEntity deviceEntity = DeviceEntity.builder()
                .deviceType(DeviceType.SENSOR) // 센서 타입으로 지정
                .gatewaysId(gatewayId) // 패킷이 거쳐온 게이트웨이 ID를 입력
                .groupId(groupId) // 소속 그룹아이디 주입
                .deviceEui(deviceEui) // 센서의 고유 시리얼 번호(EUI)를 입력
                .deviceName(nolDeviceName) // 패킷 정보 기반의 임시 이름(예: "Temp_Sensor_01")을 입력
                .locationsId(null) // 설치 장소는 아직 모르므로 일단 null로 비움
                .lastSeenAt(OffsetDateTime.now()) // 첫 데이터가 도착했으니 통신 시각을 현재로 기록
                .createdAt(OffsetDateTime.now()) // 생성 시각을 현재로 저장
                .build();

        // [2단계] 센서 정보를 sensor_devices DB 테이블에 저장
        DeviceEntity savedDevice = deviceRepository.save(deviceEntity);

        // [3단계] 패킷 안에 있던 데이터 항목들(예: ["co2", "temperature"])을 확인해 속성(Attribute) 테이블도 채움
        if (metricKeys != null && !metricKeys.isEmpty()) {
            List<DeviceAttributeEntity> attributes = metricKeys.stream()
                    .map(metricKey -> DeviceAttributeEntity.builder()
                            .deviceId(savedDevice) // 방금 DB에 저장한 센서(부모)와 연결 (FK 매핑).
                            .groupId(groupId) // 속성 엔티티에도 그룹 주입
                            .metricKey(metricKey) // 수집 항목 키(예: "co2")를 저장
                            .build())
                    .toList();

            // sensor_device_attributes DB 테이블에 속성들을 한 번에 저장
            deviceAttributeRepository.saveAll(attributes);
        }

        // [4단계] 캐시에 올릴 경량화 객체(DeviceCacheEntry)를 생성
        DeviceCacheEntry cacheEntry = new DeviceCacheEntry(
                savedDevice.getDeviceId(), // 기기 PK 번호
                savedDevice.getDeviceEui(), // 고유 EUI
                savedDevice.getGatewaysId(), // 게이트웨이 ID
                savedDevice.getLocationsId() // 위치 ID (현재는 null)
        );

        // [5단계] 메모리 캐시(ConcurrentHashMap)에 적재하여 다음 패킷부터는 DB 조회 없이 빠르게 처리
        deviceLookupCacheService.populate(cacheEntry);

        // [6단계] 생성된 캐시 엔트리를 반환
        return cacheEntry;
    }

//    @Override
//    @Transactional // REST API 기반 ACTUATOR 수동 등록
//    public Long createActuator(DeviceRequest request) {
//        // ACTUATOR 타입으로 지정하고 센서 전용 필드는 모두 null 처리
//        DeviceEntity deviceEntity = DeviceEntity.builder()
//                .deviceType(DeviceType.ACTUATOR)
//                .deviceName(request.deviceName())
//                .locationsId(request.locationId())
//                .gatewaysId(null)
//                .deviceEui(null)
//                .lastSeenAt(null)
//                .createdAt(OffsetDateTime.now())
//                .build();
//
//        return deviceRepository.save(deviceEntity).getDeviceId();
//    }

    @Override
    public DeviceResponse getDeviceById(Long deviceId) {
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        return toDto(deviceEntity);
    }

    @Override
    @Transactional // 위치 수정 로직
    public void updateDeviceLocation(Long deviceId, Long newLocationId) {
        if (newLocationId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        deviceEntity.updateLocation(newLocationId);

        // 캐시도 같이 갱신 (deviceEui가 있는 센서만 캐시에 들어있음)
        if(deviceEntity.getDeviceEui() != null){
            deviceLookupCacheService.updateLocation(deviceEntity.getDeviceEui(), newLocationId);
        }
    }

    @Override
    @Transactional // 이름 수정 로직
    public void updateDeviceName(Long deviceId, String newDeviceName) {
        if (newDeviceName == null || newDeviceName.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        deviceEntity.updateName(newDeviceName);
    }

    @Override
    @Transactional // 센서 통신 시각 최신화 로직
    public void handlePacketReceived(String deviceEui) {
        if (deviceEui == null || deviceEui.trim().isEmpty()) {
            return;
        }
        deviceRepository.findByDeviceEui(deviceEui)
                .ifPresent(DeviceEntity::updateLastSeen);
    }

    @Override
    @Transactional // 삭제 시 부모/자식 테이블 안전 삭제
    public void deleteDevice(Long deviceId) {
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));

        // @Query 없이 완벽하게 동작하는 자식 테이블 일괄 삭제 메서드 호출
        deviceAttributeRepository.deleteByDeviceId_DeviceId(deviceId);
        deviceRepository.delete(deviceEntity);

        // 캐시에서도 제거
        if(deviceEntity.getDeviceEui() != null){
            deviceLookupCacheService.evict(deviceEntity.getDeviceEui());
        }
    }

    @Override
    @Transactional
    public void deleteAll() {
        // DB삭제 캐시 삭제
        deviceAttributeRepository.deleteAll();
        deviceRepository.deleteAll();
        deviceLookupCacheService.clear();
    }

    @Override
    public List<DeviceResponse> searchDevices(Long id, String eui, Long locationId, Long gatewayId, String deviceName) {
        List<DeviceEntity> entities;

        if (id != null) {
            entities = deviceRepository.findById(id).map(List::of).orElse(List.of());
        } else if (eui != null && !eui.trim().isEmpty()) {
            entities = deviceRepository.findByDeviceEui(eui).map(List::of).orElse(List.of());
        } else if (locationId != null) {
            entities = deviceRepository.findByLocationsId(locationId);
        } else if (gatewayId != null) {
            entities = deviceRepository.findByGatewaysId(gatewayId);
        } else if (deviceName != null && !deviceName.trim().isEmpty()) {
            entities = deviceRepository.findByDeviceName(deviceName);
        } else {
            entities = deviceRepository.findAll();
        }

        return entities.stream().map(this::toDto).toList();
    }

    private DeviceResponse toDto(DeviceEntity e) {
        return new DeviceResponse(
                e.getDeviceId(),
                e.getDeviceType(),
                e.getGatewaysId(),
                e.getLocationsId(),
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