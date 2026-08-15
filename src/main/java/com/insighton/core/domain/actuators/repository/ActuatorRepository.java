package com.insighton.core.domain.actuators.repository;

import com.insighton.core.domain.actuators.entity.Actuator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// ActuatorEntity에 대한 DB 접근을 담당하는 리포지토리 인터페이스
public interface ActuatorRepository extends JpaRepository<Actuator, Long> {

    // 특정 위치(locationId)에 속한 모든 액추에이터 조회
    List<Actuator> findByLocationLocationId(Long locationId);

    // 액추에이터 종류(typeCode)별 목록 조회
    List<Actuator> findByActuatorType(String actuatorType);

    // 위치 기반 액추에이터 타입 조회
    List<Actuator> findByLocationLocationIdAndActuatorType(Long locationId, String actuatorType);

    // 특정 액추에이터 종류를 사용 중인 액추에이터 수 조회 - 종류 삭제 전 사용 여부 확인용
    long countByActuatorType(String actuatorType);

    // 장소 삭제용
    void deleteAllByLocationLocationId(Long locationId);

    // 장소 리스트 = 그룹 삭제용
    void deleteAllByLocationLocationIdIn(List<Long> locationIds);
}
