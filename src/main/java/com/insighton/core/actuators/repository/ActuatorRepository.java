package com.insighton.core.actuators.repository;

import com.insighton.core.actuators.entity.ActuatorType;
import com.insighton.core.actuators.entity.Actuator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// ActuatorEntity에 대한 DB 접근을 담당하는 리포지토리 인터페이스
public interface ActuatorRepository extends JpaRepository<Actuator, Long> {

    // 특정 위치(locationId)에 속한 모든 액추에이터 조회
    List<Actuator> findByLocationId_LocationId(Long locationId);

    // 액추에이터 종류(ActuatorType)별 목록 조회
    List<Actuator> findByActuatorType(ActuatorType actuatorType);

    // 장소 삭제용
    void deleteAllByLocationId(Long locationId);

    // 장소 리스트 = 그룹 삭제용
    void deleteAllByLocationIdList(List<Long> locationIds);
}
