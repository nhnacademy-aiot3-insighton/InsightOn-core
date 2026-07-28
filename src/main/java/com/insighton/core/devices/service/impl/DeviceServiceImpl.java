package com.insighton.core.devices.service.impl;

import com.insighton.core.device_attributes.entity.DeviceAttributeEntity;
import com.insighton.core.device_attributes.repository.DeviceAttributeRepository;
import com.insighton.core.devices.dto.DeviceRequest;
import com.insighton.core.devices.dto.DeviceResponse;
import com.insighton.core.devices.entity.DeviceCacheEntry;
import com.insighton.core.devices.entity.DeviceEntity;

import com.insighton.core.devices.entity.DeviceType;
import com.insighton.core.devices.repository.DeviceRepository;
import com.insighton.core.devices.service.DeviceLookupCacheService;
import com.insighton.core.devices.service.DeviceService;
import com.insighton.core.exception.CustomException;
import com.insighton.core.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service // 디바이스 비즈니스 로직을 서빙하는 스프링 서비스 빈으로 등록
@RequiredArgsConstructor // final 필드들에 대한 생성자를 자동으로 생성하여 의존성 주입
@Transactional(readOnly = true) // 읽기 전용 트랜잭션을 기본으로 설정하여 조회 성능 최적화
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository; // 디바이스 엔티티 접근용 리포지토리 의존성 주입
    private final DeviceAttributeRepository deviceAttributeRepository; // 디바이스 속성 엔티티 접근용 리포지토리 의존성 주입
    private final DeviceLookupCacheService deviceLookupCacheService; // MQTT 패킷 조회용 캐시 서비스 의존성 주입

    @Override
    @Transactional // DB 데이터 생성 및 변경이 발생하므로 쓰기 가능한 트랜잭션 적용
    public DeviceCacheEntry autoProvision(Long gatewayId, String deviceEui, String deviceName, Set<String> metricKeys) {
        // SENSOR 타입으로 신규 디바이스 엔티티 객체 생성 (locationsId는 null로 초기 세팅)
        DeviceEntity deviceEntity = DeviceEntity.builder()
                .deviceType(DeviceType.SENSOR) // 디바이스 타입을 SENSOR로 지정
                .gatewaysId(gatewayId) // 패킷이 거쳐온 상위 게이트웨이 ID 설정
                .deviceEui(deviceEui) // 하드웨어 고유 식별자 DevEUI 설정
                .deviceName(deviceName) // 수신된 패킷 기반의 디바이스 이름 설정
                .locationsId(null) // 초기 위치 정보는 null로 세팅
                .lastSeenAt(OffsetDateTime.now()) // 최근 통신 시각을 현재 시각으로 설정
                .createdAt(OffsetDateTime.now()) // 디바이스 최초 생성 시각 설정
                .build();

        DeviceEntity savedDevice = deviceRepository.save(deviceEntity); // SENSOR 디바이스 DB 영속화

        // 패킷 내부에서 추출한 메트릭 키 집합이 존재할 경우 디바이스 속성 일괄 생성
        if (metricKeys != null && !metricKeys.isEmpty()) {
            List<DeviceAttributeEntity> attributes = metricKeys.stream()
                    .map(metricKey -> DeviceAttributeEntity.builder()
                            .deviceId(savedDevice) // 속성을 소유할 부모 디바이스 매핑
                            .metricKey(metricKey) // 패킷 메트릭 키 문자열 설정
                            .build())
                    .toList();

            deviceAttributeRepository.saveAll(attributes); // 연관된 모든 메트릭 속성을 DB에 저장
        }

        // 캐시 적재에 사용할 DeviceCacheEntry 객체 생성
        DeviceCacheEntry cacheEntry = new DeviceCacheEntry(
                savedDevice.getDeviceId(), // 생성된 디바이스 PK
                savedDevice.getDeviceEui(), // 고유 DevEUI
                savedDevice.getGatewaysId(), // 게이트웨이 ID
                savedDevice.getLocationsId() // 위치 ID (null)
        );

        deviceLookupCacheService.populate(cacheEntry); // 빠른 조회를 위해 캐시 서비스에 엔트리 적재

        return cacheEntry; // 생성된 캐시 엔트리 반환
    }

    @Override
    @Transactional // DB 저장을 위한 쓰기 트랜잭션 처리
    public Long createDevice(DeviceRequest request) {
        // 웹 화면에서 "에어컨/스위치 추가" 버튼을 눌렀을 때 실행되는 액추에이터 전용 메서드
        DeviceEntity deviceEntity = DeviceEntity.builder()
                .deviceType(DeviceType.ACTUATOR) // 1. 무조건 기기 타입을 ACTUATOR로 고정
                .deviceName(request.deviceName()) // 2. 사용자가 입력한 기기 이름을 저장
                .locationsId(request.locationsId()) // 3. 사용자가 지정한 설치 위치 ID를 저장
                .gatewaysId(null) // 4. 센서 전용 필드인 gatewayId는 필요 없으므로 null을 입력
                .deviceEui(null) // 5. 센서 전용 필드인 deviceEui는 필요 없으므로 null을 입력
                .lastSeenAt(null) // 6. 센서 전용 필드인 통신 시각도 null을 입력
                .createdAt(OffsetDateTime.now()) // 7. 생성 시간을 현재 시각으로 저장저장
                .build();

        // DB에 액추에이터를 저장하고 생성된 PK ID를 반환합니다.
        return deviceRepository.save(deviceEntity).getDeviceId();
    }

    @Override
    public DeviceResponse getDeviceById(Long deviceId) {
        // ID로 디바이스를 조회하고, 없을 경우 DEVICE_NOT_FOUND 예외 발생
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        return toDto(deviceEntity); // 엔티티를 응답 DTO로 변환하여 반환
    }

    @Override
    public void updateDeviceLocation(Long deviceId, Long newLocationId) {

    }

    @Override
    public void updateDeviceName(Long deviceId, String newDeviceName) {

    }

    @Override
    @Transactional // 통신 시각 updates를 위해 쓰기 트랜잭션 적용
    public void handlePacketReceived(String deviceEui) {
        // 수신된 EUI가 유효하지 않으면 동작 없이 리턴
        if (deviceEui == null || deviceEui.trim().isEmpty()) {
            return;
        }
        // DevEUI로 디바이스를 찾아 최근 통신 시각을 현재로 업데이트
        deviceRepository.findByDeviceEui(deviceEui)
                .ifPresent(DeviceEntity::updateLastSeen);
    }

    @Override
    @Transactional // 단일 삭제를 위한 쓰기 트랜잭션 적용
    public void deleteDevice(Long deviceId) {
        // 삭제할 디바이스 조회 후 없으면 예외 발생
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));

        deviceAttributeRepository.deleteByDeviceId_DeviceId(deviceId); // FK 위반 방지를 위해 자식 속성 데이터 선삭제
        deviceRepository.delete(deviceEntity); // 부모 디바이스 엔티티 삭제
    }

    @Override
    @Transactional // 전체 삭제를 위한 쓰기 트랜잭션 적용
    public void deleteAll() {
        deviceAttributeRepository.deleteAll(); // 전체 자식 속성 선삭제
        deviceRepository.deleteAll(); // 전체 디바이스 삭제
    }

    @Override
    public List<DeviceResponse> searchDevices(Long id, String eui, Long locationId, Long gatewayId, String deviceName) {
        List<DeviceEntity> entities; // 조회된 엔티티들을 담을 리스트 선언

        // 검색 조건의 우선순위에 따라 단일 검색 수행
        if (id != null) {
            entities = deviceRepository.findById(id).map(List::of).orElse(List.of()); // ID 조회 결과를 리스트로 생성
        } else if (eui != null && !eui.trim().isEmpty()) {
            entities = deviceRepository.findByDeviceEui(eui).map(List::of).orElse(List.of()); // EUI 조회 결과를 리스트로 생성
        } else if (locationId != null) {
            entities = deviceRepository.findByLocationsId(locationId); // 위치 ID 기반 목록 조회
        } else if (gatewayId != null) {
            entities = deviceRepository.findByGatewaysId(gatewayId); // 게이트웨이 ID 기반 목록 조회
        } else if (deviceName != null && !deviceName.trim().isEmpty()) {
            entities = deviceRepository.findByDeviceName(deviceName); // 장비 이름 기반 목록 조회
        } else {
            entities = deviceRepository.findAll(); // 모든 조건이 없을 경우 전체 디바이스 조회
        }

        return entities.stream().map(this::toDto).toList(); // 조회된 엔티티 리스트를 DTO 리스트로 변환하여 반환
    }

    // DeviceEntity를 DeviceResponse DTO로 변환해주는 프라이빗 헬퍼 메서드
    private DeviceResponse toDto(DeviceEntity e) {
        return new DeviceResponse(
                e.getDeviceId(), // 장치 PK ID
                e.getDeviceType(), // 디바이스 타입 (SENSOR / ACTUATOR)
                e.getGatewaysId(), // 게이트웨이 ID
                e.getLocationsId(), // 위치 ID
                e.getDeviceEui(), // 고유 DevEUI
                e.getDeviceName(), // 장비 이름
                e.getCreatedAt(), // 최초 생성 일시
                e.getLastSeenAt() // 최근 통신 일시
        );
    }
}