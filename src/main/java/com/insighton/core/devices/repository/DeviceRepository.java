package com.insighton.core.devices.repository;

import com.insighton.core.devices.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {
    List<DeviceEntity> findByGatewaysId(Long gatewaysId);
    List<DeviceEntity> findByLocationsId(Long locationsId);

    // findByName -> findByDeviceName으로 통일
    List<DeviceEntity> findByDeviceName(String deviceName);

    Optional<DeviceEntity> findByDeviceEui(String deviceEui);
}