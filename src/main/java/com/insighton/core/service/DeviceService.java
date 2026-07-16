package com.insighton.core.service;

import com.insighton.core.dto.DeviceResponseDto;
import com.insighton.core.entity.DeviceEntity;
import com.insighton.core.error.NoDeviceId;
import com.insighton.core.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {
    private final DeviceRepository deviceRepository;

    public DeviceResponseDto getDevice(Long deviceId){
        DeviceEntity deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NoDeviceId(deviceId + "를 찾을 수 없음"));

//        List<DeviceAttributeDto> attributeDtoList = deviceEntity.getAttributeList().stream()
//                .map(attr -> new DeviceAttributeDto(
//                        attr.getMetricKey(),
//                        attr.getDisplayName(),
//                        attr.getUnit(),
//                        attr.getCurrentValueSte()))
//                .toList();

        return new DeviceResponseDto(
                deviceEntity.getDeviceId(),
                deviceEntity.getGatewaysId(),
                deviceEntity.getLocationsId(),
                deviceEntity.getDeviceEui(),
                deviceEntity.getName(),
                deviceEntity.getType(),
                deviceEntity.getCreatedAt());
    }

    @Transactional
    public void deleteDevice(Long deviceId){
        deviceRepository.deleteById(deviceId);
    }
}
