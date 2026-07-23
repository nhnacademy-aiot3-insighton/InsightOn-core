package com.insighton.core.service;


import com.insighton.core.dto.DeviceRequestDto;
import com.insighton.core.dto.DeviceResponseDto;
import com.insighton.core.entity.DeviceEntity;
import com.insighton.core.error.CustomException;
import com.insighton.core.error.ErrorCode;
import com.insighton.core.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {
    private final DeviceRepository deviceRepository;


    // DTO기반 엔티티 생성 저장
    @Transactional
    public Long createDevice(DeviceRequestDto request){
        DeviceEntity deviceEntity = DeviceEntity.builder()
                .deviceName(request.deviceName()) // name -> deviceName
                .type(request.type())
                .deviceEui(request.deviceEui())
                .gatewaysId(request.gatewayId())
                .locationsId(request.locationsId())
                .createdAt(ZonedDateTime.now())
                .build();
        return deviceRepository.save(deviceEntity).getDeviceId();
    }

    // 단건 기기 조회 전용 메서드 (컨트롤러에서 사용)
    // 기기가 없으면 500 에러 대신 404를 내보낼 수 있도록 예외를 던집니다.
    public DeviceResponseDto getDeviceById(Long deviceId) {
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        return toDto(deviceEntity);
    }

    // 위치 이동 및 수정
    // 수정할 기기 ID로 엔티티를 조회한 후, 엔티티 내부의 변경 메서드를 호출하여 위치ID갱신
    @Transactional
    public void updateDeviceLocation(Long deviceId, Long newLocationId){
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow( () -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        deviceEntity.updateLocation(newLocationId);// 엔티티 내부 메서드
    }

    // 외부기기나 MQTT리스너로부터 센서 패킷이 수신되었을 때, 해당 기기가 살이있는지 기록
    // 패킷에 포함된 deviceEui를 이용해 기기를 찾아낸 뒤 마지막 통신 시각을 현재 시간으로 업데이트
    // 별도위 이력 테이블을 두지 않고 필드만 갱신 (부하 감소)
    @Transactional
    public void handlePacketReceived(String deviceEui){
        deviceRepository.findByDeviceEui(deviceEui)
                .ifPresent(DeviceEntity::updateLastSeen);
    }

    // 💡 [수정됨] deviceId로 기기 삭제
    // 무작정 지우지 않고 DB에 기기가 존재하는지 먼저 검증합니다.
    @Transactional
    public void deleteDevice(Long deviceId){
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        deviceRepository.delete(deviceEntity);
    }

    // 전체 삭제
    @Transactional
    public void deleteAll(){
        deviceRepository.deleteAll();
    }

    // 사용자가 ID,EUI,위치,게이트웨이,이름 등 다양한 조건중 어떤것이든 검색 대응가능
    // 입력된 파라미터의 우선숭위나 조건에 따라 적절한 레파지토리 메서드를 호출하여 데이터를 조회한뒤 이를 DTO리스트로 변환 반환
    public List<DeviceResponseDto> searchDevices(Long id, String eui, Long locationId, Long gatewayId, String name) {
        List<DeviceEntity> entities;

        // 경우의 수 1: ID가 있으면 ID로 즉시 반환
        if (id != null) {
            entities = deviceRepository.findById(id).map(List::of).orElse(List.of());
        }
        // 경우의 수 2: EUI가 있으면 EUI로 검색
        else if (eui != null) {
            entities = deviceRepository.findByDeviceEui(eui).map(List::of).orElse(List.of());
        }
        // 경우의 수 3: 로케이션 ID 검색
        else if (locationId != null) {
            entities = deviceRepository.findByLocationsId(locationId);
        }
        // 경우의 수 4: 게이트웨이 ID 검색
        else if (gatewayId != null) {
            entities = deviceRepository.findByGatewaysId(gatewayId);
        }
        // 경우의 수 5: 이름 검색
        else if (name != null) {
            entities = deviceRepository.findByDeviceName(name); // name -> deviceName
        }
        else {
            entities = deviceRepository.findAll();
        }

        // 엔티티를 DTO로 변환
        return entities.stream().map(this::toDto).toList();
    }

    private DeviceResponseDto toDto(DeviceEntity e) {
        return new DeviceResponseDto(
                e.getDeviceId(),
                e.getGatewaysId(),
                e.getLocationsId(),
                e.getDeviceEui(),
                e.getDeviceName(), // name -> deviceName
                e.getType(),
                e.getCreatedAt(),
                e.getLastSeenAt());
    }
}