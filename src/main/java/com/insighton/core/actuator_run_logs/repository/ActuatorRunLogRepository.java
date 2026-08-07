package com.insighton.core.actuator_run_logs.repository;

import com.insighton.core.actuator_run_logs.entity.ActuatorRunLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface ActuatorRunLogRepository extends JpaRepository<ActuatorRunLog, Long> {

    // 액추에이터별 실행 이력 최신순 조회
    Page<ActuatorRunLog> findByActuatorActuatorIdOrderByExecutedAtDesc(Long actuatorId, Pageable pageable);

    // JOIN FETCH로 actuator, locationId를 한번에 즉시 로딩
    @Query("""
        SELECT r FROM ActuatorRunLog r
        JOIN FETCH r.actuator a
        JOIN FETCH a.location l
        WHERE l.locationId IN :locationIds
        AND r.executedAt BETWEEN :from AND :to
        ORDER BY r.executedAt DESC
        """)
    List<ActuatorRunLog> findForReport(
            @Param("locationIds") List<Long> locationIds,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);


    // 그룹 삭제할 때 - 그룹 소속 장소ID 리스트를 넘겨서, 그 장소들에 딸린 액추에이터들의 로그 일괄 삭제 (그룹용 겸용)
    void deleteAllByActuatorLocationLocationIdIn(List<Long> locationIds);

    // 장소 하나 삭제할 때 - 그 장소 소속 액추에이터들의 로그 삭제
    void deleteAllByActuatorLocationLocationId(Long locationId);

    // 액추에이터 하나만 삭제할 때
    void deleteByActuatorActuatorId(Long actuatorId);
}