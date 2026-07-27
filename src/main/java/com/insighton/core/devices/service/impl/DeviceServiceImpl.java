package com.insighton.core.devices.service.impl;

import com.insighton.core.exception.CustomException;
import com.insighton.core.exception.ErrorCode;
import com.insighton.core.devices.dto.DeviceRequest;
import com.insighton.core.devices.dto.DeviceResponse;
import com.insighton.core.devices.entity.DeviceEntity;
import com.insighton.core.devices.repository.DeviceRepository;
import com.insighton.core.devices.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service // 디바이스 비즈니스 로직을 처리하는 스프링 서비스 빈 등록
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션 적용하여 조회 성능 최적화
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository; // 디바이스 데이터베이스 접근을 위한 리포지토리 주입

    @Override
    @Transactional // 데이터 변경 작업이 발생하므로 쓰기 가능한 트랜잭션 적용
    public Long createDevice(DeviceRequest request) {
        // 동일한 Device EUI가 이미 DB에 존재하는지 중복 검사
        if(deviceRepository.findByDeviceEui(request.deviceEui()).isPresent()){
            throw new CustomException(ErrorCode.DUPLICATE_DEVICE_EUI); // EUI 중복 시 예외 발생
        }
        // 전달받은 DTO 정보를 바탕으로 새로운 디바이스 엔티티 객체 생성
        DeviceEntity deviceEntity = DeviceEntity.builder()
                .deviceName(request.deviceName()) // 장치 이름 매핑
                .deviceEui(request.deviceEui()) // 디바이스 고유 EUI 매핑
                .gatewaysId(request.gatewayId()) // 상위 게이트웨이 ID 매핑
                .locationsId(request.locationsId()) // 설치 위치 ID 매핑
                .createdAt(OffsetDateTime.now()) // 현재 시각을 생성일시로 설정
                .build();
        return deviceRepository.save(deviceEntity).getDeviceId(); // DB에 저장 후 생성된 PK(deviceId) 반환
    }

    @Override
    public DeviceResponse getDeviceById(Long deviceId) {
        // ID로 디바이스를 조회하고, 없으면 DEVICE_NOT_FOUND 예외 발생
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        return toDto(deviceEntity); // 조회된 엔티티를 클라이언트 응답용 DTO로 변환하여 반환
    }

    @Override
    @Transactional // 엔티티 수정 작업을 위해 쓰기 트랜잭션 적용
    public void updateDeviceLocation(Long deviceId, Long newLocationId) {
        // 변경할 위치 ID가 null인 경우 유효하지 않은 입력 예외 처리
        if (newLocationId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        // 수정할 디바이스 조회 후 없으면 예외 발생
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        deviceEntity.updateLocation(newLocationId); // 엔티티 내부 메서드를 통한 위치 정보 수정 (Dirty Checking)
    }

    @Override
    @Transactional // 엔티티 수정 작업을 위해 쓰기 트랜잭션 적용
    public void updateDeviceName(Long deviceId, String newDeviceName) {
        // 변경할 이름이 null이거나 공백인 경우 유효하지 않은 입력 예외 처리
        if (newDeviceName == null || newDeviceName.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        // 수정할 디바이스 조회 후 없으면 예외 발생
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        deviceEntity.updateName(newDeviceName); // 엔티티 내부 메서드를 통한 이름 수정 (Dirty Checking)
    }

    @Override
    @Transactional // 수신 시각 업데이트를 위한 쓰기 트랜잭션 적용
    public void handlePacketReceived(String deviceEui) {
        // EUI로 디바이스를 찾은 경우에만 마지막 통신 시각을 현재 시각으로 갱신
        deviceRepository.findByDeviceEui(deviceEui)
                .ifPresent(DeviceEntity::updateLastSeen);
    }

    @Override
    @Transactional // 삭제 작업을 위해 쓰기 트랜잭션 적용
    public void deleteDevice(Long deviceId) {
        // 삭제 대상 디바이스 조회 후 없으면 예외 발생
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        deviceRepository.delete(deviceEntity); // DB에서 해당 디바이스 데이터 삭제
    }

    @Override
    @Transactional // 전체 삭제 작업을 위해 쓰기 트랜잭션 적용
    public void deleteAll() {
        deviceRepository.deleteAll(); // DB 내의 모든 디바이스 데이터 삭제
    }

    @Override
    public List<DeviceResponse> searchDevices(Long id, String eui, Long locationId, Long gatewayId, String name) {
        List<DeviceEntity> entities; // 조회 결과를 담을 엔티티 리스트 선언

        // 검색 조건의 우선순위에 따라 단일 조건 조회를 수행
        if (id != null) {
            entities = deviceRepository.findById(id).map(List::of).orElse(List.of()); // ID 조회 결과 리스트 생성
        } else if (eui != null) {
            entities = deviceRepository.findByDeviceEui(eui).map(List::of).orElse(List.of()); // EUI 조회 결과 리스트 생성
        } else if (locationId != null) {
            entities = deviceRepository.findByLocationsId(locationId); // 위치 ID 기반 목록 조회
        } else if (gatewayId != null) {
            entities = deviceRepository.findByGatewaysId(gatewayId); // 게이트웨이 ID 기반 목록 조회
        } else if (name != null && !name.trim().isEmpty()) {
            entities = deviceRepository.findByDeviceName(name); // 장치 이름 기반 목록 조회
        } else {
            entities = deviceRepository.findAll(); // 아무 조건도 없으면 전체 디바이스 목록 조회
        }

        return entities.stream().map(this::toDto).toList(); // 조회된 엔티티 리스트를 DTO 리스트로 변환하여 반환
    }

    // 엔티티(DeviceEntity) 객체를 DTO(DeviceResponse) 객체로 변환해주는 프라이빗 헬퍼 메서드
    private DeviceResponse toDto(DeviceEntity e) {
        return new DeviceResponse(
                e.getDeviceId(), // 장치 PK ID
                e.getGatewaysId(), // 게이트웨이 ID
                e.getLocationsId(), // 위치 ID
                e.getDeviceEui(), // 고유 EUI
                e.getDeviceName(), // 장치 이름
                e.getCreatedAt(), // 생성 일시
                e.getLastSeenAt() // 마지막 통신 일시
        );
    }
}