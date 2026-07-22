package com.insighton.core.device.service;

import com.insighton.core.device.dto.DeviceRequestDto;
import com.insighton.core.device.dto.DeviceResponseDto;
import com.insighton.core.device.entity.DeviceEntity;
import com.insighton.core.device.error.CustomException;
import com.insighton.core.device.error.ErrorCode;
import com.insighton.core.device.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * IoT 장치(Device) 관리 핵심 비즈니스 로직을 처리하는 서비스 클래스.
 * <p>
 * 기본적으로 읽기 전용 트랜잭션({@code @Transactional(readOnly = true)})이 적용되어 있으며,
 * 데이터 변경이 일어나는 메서드는 별도로 {@code @Transactional}을 명시합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;

    /**
     * 새로운 장치(Device)를 등록하고 DB에 저장합니다.
     *
     * @param request 장치 등록 요청 정보 DTO
     * @return 저장 완료된 장치의 식별자 ID (deviceId)
     */
    @Transactional
    public Long createDevice(DeviceRequestDto request){
        DeviceEntity deviceEntity = DeviceEntity.builder()
                .name(request.name())
                .type(request.type())
                .deviceEui(request.deviceEui())
                .gatewaysId(request.gatewayId())
                .locationsId(request.locationsId())
                .createdAt(ZonedDateTime.now())
                .build();
        return deviceRepository.save(deviceEntity).getDeviceId();
    }

    /**
     * 특정 장치의 설치 위치(Location)를 이동하거나 수정합니다.
     * <p>
     * JPA Dirty Checking(변경 감지)을 활용하여 엔티티 필드만 변경하면 트랜잭션 종료 시점에 DB 업데이트가 수행됩니다.
     *
     * @param deviceId 수정 대상 장치 ID
     * @param newLocationId 새로 지정할 위치 ID
     * @throws CustomException 수정하려는 장치가 DB에 존재하지 않는 경우 발생
     */
    @Transactional
    public void updateDeviceLocation(Long deviceId, Long newLocationId){
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        deviceEntity.updateLocation(newLocationId); // 엔티티 내부 상태 변경
    }

    /**
     * 센서/액추에이터에서 무선 패킷이 수신되었을 때 장치의 통신 생존 상태를 업데이트합니다.
     * <p>
     * 별도의 이력 테이블에 매번 적재하지 않고 최신 통신 시각(lastSeenAt) 필드만 갱신하여 시스템 부하를 최소화합니다.
     *
     * @param deviceEui 하드웨어 고유 식별 코드 (DevEUI)
     */
    @Transactional
    public void handlePacketReceived(String deviceEui){
        deviceRepository.findByDeviceEui(deviceEui)
                .ifPresent(DeviceEntity::updateLastSeen);
    }

    /**
     * 단일 장치를 ID 기반으로 삭제합니다.
     *
     * @param deviceId 삭제할 장치 ID
     */
    @Transactional
    public void deleteDevice(Long deviceId){
        deviceRepository.deleteById(deviceId);
    }

    /**
     * 시스템 내 모든 장치를 일괄 삭제합니다.
     */
    @Transactional
    public void deleteAll(){
        deviceRepository.deleteAll();
    }

    /**
     * 동적 조건에 맞춰 장치 목록을 통합 검색합니다.
     * <p>
     * 파라미터의 입력 여부에 따라 우선순위 조건(ID > EUI > LocationId > GatewayId > Name)을 부여하여 단일 조회 메서드를 호출하며,
     * 파라미터가 모두 null일 경우 전체 목록을 조회합니다.
     *
     * @param id 장치 ID (최우선)
     * @param eui DevEUI 식별 문자열
     * @param locationId 위치 ID
     * @param gatewayId 게이트웨이 ID
     * @param name 장치 명칭
     * @return 검색 조건에 부합하는 장치 DTO 리스트
     */
    public List<DeviceResponseDto> searchDevices(Long id, String eui, Long locationId, Long gatewayId, String name) {
        List<DeviceEntity> entities;

        // 경우의 수 1: ID 조건 우선 검색
        if (id != null) {
            entities = deviceRepository.findById(id).map(List::of).orElse(List.of());
        }
        // 경우의 수 2: DevEUI 조건 검색
        else if (eui != null) {
            entities = deviceRepository.findByDeviceEui(eui).map(List::of).orElse(List.of());
        }
        // 경우의 수 3: 소속 위치 ID 기준 검색
        else if (locationId != null) {
            entities = deviceRepository.findByLocationsId(locationId);
        }
        // 경우의 수 4: 소속 게이트웨이 ID 기준 검색
        else if (gatewayId != null) {
            entities = deviceRepository.findByGatewaysId(gatewayId);
        }
        // 경우의 수 5: 장치 이름 기준 검색
        else if (name != null) {
            entities = deviceRepository.findByName(name);
        }
        // 조건이 없는 경우 전체 조회
        else {
            entities = deviceRepository.findAll();
        }

        // 엔티티 목록을 Response DTO 목록으로 변환하여 반환
        return entities.stream().map(this::toDto).toList();
    }

    /**
     * DeviceEntity 객체를 DeviceResponseDto 객체로 매핑하는 변환 메서드
     *
     * @param e 장치 엔티티
     * @return 변환된 DTO
     */
    private DeviceResponseDto toDto(DeviceEntity e) {
        return new DeviceResponseDto(
                e.getDeviceId(),
                e.getGatewaysId(),
                e.getLocationsId(),
                e.getDeviceEui(),
                e.getName(),
                e.getType(),
                e.getCreatedAt(),
                e.getLastSeenAt());
    }
}