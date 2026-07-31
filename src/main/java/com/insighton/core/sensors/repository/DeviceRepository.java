package com.insighton.core.sensors.repository;

import com.insighton.core.sensors.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {
    // 연관관계 객체를 뚫고 들어가 ID로 조회하려면 _언더스코어 네비게이션 필요
    List<DeviceEntity> findByGatewaysId_GatewayId(Long gatewayId);
    List<DeviceEntity> findByLocationsId_LocationId(Long locationId);
    List<DeviceEntity> findByGroupId_GroupId(Long groupId);

    // findByName -> findByDeviceName으로 통일
    List<DeviceEntity> findByDeviceName(String deviceName);

    Optional<DeviceEntity> findByDeviceEui(String deviceEui);

}