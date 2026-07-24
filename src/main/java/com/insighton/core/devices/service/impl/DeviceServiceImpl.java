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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;

    @Override
    @Transactional
    public Long createDevice(DeviceRequest request) {
        // eui 중복체크
        if(deviceRepository.findByDeviceEui(request.deviceEui()).isPresent()){
            throw new CustomException(ErrorCode.DUPLICATE_DEVICE_EUI);
        }
        DeviceEntity deviceEntity = DeviceEntity.builder()
                .deviceName(request.deviceName())
                .deviceEui(request.deviceEui())
                .gatewaysId(request.gatewayId())
                .locationsId(request.locationsId())
                .createdAt(OffsetDateTime.now())
                .build();
        return deviceRepository.save(deviceEntity).getDeviceId();
    }

    @Override
    public DeviceResponse getDeviceById(Long deviceId) {
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        return toDto(deviceEntity);
    }

    @Override
    @Transactional
    public void updateDeviceLocation(Long deviceId, Long newLocationId) {
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        deviceEntity.updateLocation(newLocationId);
    }

    @Override
    @Transactional
    public void handlePacketReceived(String deviceEui) {
        deviceRepository.findByDeviceEui(deviceEui)
                .ifPresent(DeviceEntity::updateLastSeen);
    }

    @Override
    @Transactional
    public void deleteDevice(Long deviceId) {
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        deviceRepository.delete(deviceEntity);
    }

    @Override
    @Transactional
    public void deleteAll() {
        deviceRepository.deleteAll();
    }

    @Override
    public List<DeviceResponse> searchDevices(Long id, String eui, Long locationId, Long gatewayId, String name) {
        List<DeviceEntity> entities;

        if (id != null) {
            entities = deviceRepository.findById(id).map(List::of).orElse(List.of());
        } else if (eui != null) {
            entities = deviceRepository.findByDeviceEui(eui).map(List::of).orElse(List.of());
        } else if (locationId != null) {
            entities = deviceRepository.findByLocationsId(locationId);
        } else if (gatewayId != null) {
            entities = deviceRepository.findByGatewaysId(gatewayId);
        } else if (name != null && !name.trim().isEmpty()) {
            entities = deviceRepository.findByDeviceName(name);
        } else {
            entities = deviceRepository.findAll();
        }

        return entities.stream().map(this::toDto).toList();
    }

    private DeviceResponse toDto(DeviceEntity e) {
        return new DeviceResponse(
                e.getDeviceId(),
                e.getGatewaysId(),
                e.getLocationsId(),
                e.getDeviceEui(),
                e.getDeviceName(),
                e.getCreatedAt(),
                e.getLastSeenAt()
        );
    }
}