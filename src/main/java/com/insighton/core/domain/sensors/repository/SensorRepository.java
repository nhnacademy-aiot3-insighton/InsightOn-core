package com.insighton.core.domain.sensors.repository;

import com.insighton.core.domain.sensors.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;
import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Long>, QuerydslPredicateExecutor<Sensor> {

    // 연관관계 객체를 뚫고 들어가 ID로 조회하려면 _언더스코어 네비게이션 필요
    // 위치 ID로 조회
    List<Sensor> findByLocationLocationName(String locationName);

    // 그룹 ID로 조회
    List<Sensor> findByGroupGroupId(Long groupId);

    // 센서 이름으로 조회
    List<Sensor> findBySensorName(String sensorName);

    // EUI로 단건 조회
    Optional<Sensor> findBySensorEui(String sensorEui);

    // gatewayId로 sensor들 조회 Gateway brokerUrls 삭제 시 사용
    List<Sensor> findByGatewayGatewayId(Long gatewayId);

    // 장소 삭제용
    void deleteAllByLocationLocationId(Long locationId);

    // 그룹 삭제용
    void deleteAllByGroupGroupId(Long groupId);

    // location 삭제를 위한 찾기
    List<Sensor> findByGroupGroupIdAndLocationLocationId(Long groupId, Long locationId);

    // 장소 미배정(location이 null인) 센서만 조회 - 자동 등록됐지만 아직 설치 위치를 안 정한 센서 파악용
    List<Sensor> findByGroupGroupIdAndLocationIsNull(Long groupId);

}