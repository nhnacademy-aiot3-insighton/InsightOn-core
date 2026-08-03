package com.insighton.core.actuator_run_logs.repository;

import com.insighton.core.actuator_run_logs.entity.ActuatorRunLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActuatorRunLogRepository extends JpaRepository<ActuatorRunLog, Long> {

    // 액추에이터별 실행 리력 최신순 조회
    Page<ActuatorRunLog> findByActuatorActuatorIdOrderByExecutedAtDesc(Long actuatorId, Pageable pageAble);

    // 위치 목록 범위로 실행 로그 일괄 삭제
    void deleteAllByActuatorLocationId_LocationIdIn(List<Long> locationIds);

    // 단일 위치 범위로 실행 로그 일괄 삭제
    void deleteAllByActuatorLocationId_LocationId(Long locationId);
}
