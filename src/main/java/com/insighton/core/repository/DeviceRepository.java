package com.insighton.core.repository;

import com.insighton.core.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {
    List<DeviceEntity> findByGatewaysId(Long gatewaysId);
    List<DeviceEntity> findByLocationsId(Long locationsId);
    List<DeviceEntity> findByName(String name);
    List<DeviceEntity> findByType(String type);
    Optional<DeviceEntity> findByDeviceEui(String deviceEui);

}

