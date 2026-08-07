package com.insighton.core.actuator_run_logs.service;

import com.insighton.core.actuator_run_logs.dto.ActuatorRunLogInternalResponse;
import com.insighton.core.actuator_run_logs.dto.ActuatorRunLogResponse;
import com.insighton.core.actuator_run_logs.entity.ExecutedByType;
import com.insighton.core.actuators.entity.Actuator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface ActuatorRunLogService {

    // 상태 변경 성공 직후 호출 - newState의 각 필드마다 로그 1건씩 남김
    void recordRunLogs(Actuator actuators, Map<String, Object> newState, ExecutedByType type, Long executedByUserId);

    // 특정 액추에이터의 실행 이력 조회(최신순)
    Page<ActuatorRunLogResponse> getRunLogsByActuatorId(Long actuatorId, Pageable pageable);

    
    List<ActuatorRunLogInternalResponse> getRunLogsForReport(List<Long> locationIds, OffsetDateTime from, OffsetDateTime to);

}
