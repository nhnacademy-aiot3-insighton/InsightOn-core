package com.insighton.core.service;

import com.insighton.core.dto.DeviceResponseDto;
import com.insighton.core.entity.DeviceEntity;
import com.insighton.core.error.NoDeviceId;
import com.insighton.core.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {
    private final DeviceRepository deviceRepository;

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
            entities = deviceRepository.findByName(name);
        }
        else {
            entities = deviceRepository.findAll();
        }

        // 엔티티를 DTO로 변환
        return entities.stream().map(this::toDto).toList();
    }

    private DeviceResponseDto toDto(DeviceEntity e) {
        return new DeviceResponseDto(e.getDeviceId(), e.getGatewaysId(), e.getLocationsId(), e.getDeviceEui(), e.getName(), e.getType(), e.getCreatedAt());
    }
}