package com.insighton.core.domain.sensors.repository;

import com.insighton.core.domain.sensors.entity.Sensor;

import java.util.List;

public interface SensorRepositoryCustom {

    // 조건별 장치 검색 (id/eui/locationId/sensorName 중 있는 조건만 AND로 조합) - QueryDSL(QSensor)은
    // 이 인터페이스의 구현체(SensorRepositoryCustomImpl) 안에만 있고 서비스 계층엔 노출되지 않음
    List<Sensor> search(Long groupId, Long id, String eui, Long locationId, String sensorName);
}
