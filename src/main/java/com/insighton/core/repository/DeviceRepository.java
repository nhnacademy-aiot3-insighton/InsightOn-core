package com.insighton.core.repository;

import com.insighton.core.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {
    Optional<DeviceEntity> findByDeviceEui(String deviceEui);
    Optional<DeviceEntity> findByDeviceId(Long deviceId);
    Optional<DeviceEntity> findByGatewaysId(Long gatewaysId);
    Optional<DeviceEntity> findByLocationsId(Long locationsId);
    Optional<DeviceEntity> findByDeviceName(String deviceName);
    Optional<DeviceEntity> findByDeviceType(String deviceType);
    Optional<DeviceEntity> findByCreatedAt(ZonedDateTime createdAt);

}
