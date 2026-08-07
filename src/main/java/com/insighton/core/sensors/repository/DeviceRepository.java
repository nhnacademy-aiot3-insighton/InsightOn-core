package com.insighton.core.sensors.repository;

import com.insighton.core.sensors.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    // 연관관계 객체를 뚫고 들어가 ID로 조회하려면 _언더스코어 네비게이션 필요
    // 위치 ID로 조회
    List<Device> findByLocationLocationId(Long locationId);

    // 그룹 ID로 조회
    List<Device> findByGroupGroupId(Long groupId);

    // 디바이스 이름으로 조회
    List<Device> findByDeviceName(String deviceName);

    // EUI로 단건 조회
    Optional<Device> findByDeviceEui(String deviceEui);

    // 장소 삭제용
    void deleteAllByLocationLocationId(Long locationId);

    // 그룹 삭제용
    void deleteAllByGroupGroupId(Long groupId);

}