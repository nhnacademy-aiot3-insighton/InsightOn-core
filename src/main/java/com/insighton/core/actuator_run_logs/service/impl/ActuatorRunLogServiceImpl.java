package com.insighton.core.actuator_run_logs.service.impl;

import com.insighton.core.actuator_run_logs.dto.ActuatorRunLogResponse;
import com.insighton.core.actuator_run_logs.entity.ActuatorRunLog;
import com.insighton.core.actuator_run_logs.entity.CommandType;
import com.insighton.core.actuator_run_logs.entity.ExecutedByType;
import com.insighton.core.actuator_run_logs.repository.ActuatorRunLogRepository;
import com.insighton.core.actuator_run_logs.service.ActuatorRunLogService;
import com.insighton.core.actuators.entity.Actuator;
import com.insighton.core.influx.ActuatorStatusInfluxWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActuatorRunLogServiceImpl implements ActuatorRunLogService {
    // 실행 로그 조회, 저장
    private final ActuatorRunLogRepository actuatorRunLogRepository;
    private final ActuatorStatusInfluxWriter actuatorStatusInfluxWriter;


    @Override
    @Transactional
    public void recordRunLogs(Actuator actuator, Map<String, Object> newState, ExecutedByType executedByType, Long executedByUserId) {
        if(newState == null || newState.isEmpty()){
            return;
        }
        // 트랜잭션이 살아있는 이 메서드 안에서 지연로딩 값들을 미리 다 꺼냄 (async writer로 넘기기 전에)
        String groupId = String.valueOf(actuator.getLocationId().getGroups().getGroupId());
        String locationId = String.valueOf(actuator.getLocationId().getLocationId());
        String actuatorId = String.valueOf(actuator.getActuatorId());
        String actuatorType = actuator.getActuatorType().name();

        newState.forEach((key, value) -> CommandType.fromStateKey(key).ifPresentOrElse(
                commandType -> {
                    ActuatorRunLog logEntity = ActuatorRunLog.builder()
                            .actuator(actuator)
                            .commandType(commandType)
                            .commandValue(String.valueOf(value))
                            .executedByType(executedByType)
                            .executedByUserId(executedByUserId)
                            .executedAt(OffsetDateTime.now())
                            .build();
                    actuatorRunLogRepository.save(logEntity);

                    // 파워 상태 명령만 InfluxDB 시계열로 별도 기록
                    if(commandType == CommandType.POWER_STATUS){
                        actuatorStatusInfluxWriter.writeTransition(
                                groupId, locationId, actuatorId, actuatorType,
                                logEntity.getCommandValue(),
                                logEntity.getExecutedAt());

                        actuatorStatusInfluxWriter.writeTransition(
                                groupId, locationId, actuatorId, actuatorType,
                                logEntity.getCommandValue(), logEntity.getExecutedAt());
                    }


                },
                // 매핑 안 되는 키는 로그만 남기지 않고 넘어감
                () -> log.info("알 수 없는 제어 명령키 - 실행 로그 남기지 못함: {}",key)
        ));
    }

    @Override
    public Page<ActuatorRunLogResponse> getRunLogsByActuatorId(Long actuatorId, Pageable pageable) {
        return actuatorRunLogRepository.findByAActuatorIdOrderByExecutedAtDesc(actuatorId, pageable)
                .map(ActuatorRunLogResponse::from);
    }
}
