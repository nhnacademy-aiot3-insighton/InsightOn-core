package com.insighton.core.actuatorrunlogs.service.impl;

import com.insighton.core.actuatorrunlogs.dto.ActuatorRunLogInternalResponse;
import com.insighton.core.actuatorrunlogs.dto.ActuatorRunLogResponse;
import com.insighton.core.actuatorrunlogs.entity.ActuatorRunLog;
import com.insighton.core.actuatorrunlogs.entity.CommandType;
import com.insighton.core.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.actuatorrunlogs.event.ActuatorStatusChangedEvent;
import com.insighton.core.actuatorrunlogs.repository.ActuatorRunLogRepository;
import com.insighton.core.actuatorrunlogs.service.ActuatorRunLogService;
import com.insighton.core.actuators.entity.Actuator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActuatorRunLogServiceImpl implements ActuatorRunLogService {
    // 실행 로그 조회, 저장
    private final ActuatorRunLogRepository actuatorRunLogRepository;

    // ActuatorStatusInfluxWriter를 직접 의존하지 않고 이벤트만 발행 - 실제 Influx 기록은
    // 트랜잭션 커밋 후(AFTER_COMMIT) 리스너에서 처리되어 PG-Influx 정합성이 지켜짐
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void recordRunLogs(Actuator actuator, Map<String, Object> newState,
                              ExecutedByType executedByType, Long executedByUserId) {
        if(newState == null || newState.isEmpty()){
            return;
        }
        // 트랜잭션이 살아있는 이 메서드 안에서 지연로딩 값들을 미리 다 꺼냄 (이벤트 페이로드로 넘기기 전에)
        String groupId = String.valueOf(actuator.getLocation().getGroup().getGroupId());
        String locationId = String.valueOf(actuator.getLocation().getLocationId());
        String actuatorId = String.valueOf(actuator.getActuatorId());
        String actuatorType = actuator.getActuatorType().name();

        // 상태 맵을 순회하며 매핑되는 CommandType을 찾아 실행 로그를 저장
        newState.forEach((key, value) -> CommandType.fromStateKey(key).ifPresentOrElse(
                commandType -> {
                    // 액추에이터 실행 로그 엔티티를 생성하고 DB에 저장
                    ActuatorRunLog logEntity = ActuatorRunLog.builder()
                            .actuator(actuator)
                            .commandType(commandType)
                            .commandValue(String.valueOf(value))
                            .executedByType(executedByType)
                            .executedByUserId(executedByUserId)
                            .executedAt(OffsetDateTime.now())
                            .build();
                    actuatorRunLogRepository.save(logEntity);

                    // 파워 상태 명령만 InfluxDB 시계열 대상 - 이벤트만 발행하고 실제 쓰기는 트랜잭션 커밋 후로 미룸
                    if (commandType == CommandType.POWER_STATUS) {
                        eventPublisher.publishEvent(new ActuatorStatusChangedEvent(
                                groupId, locationId, actuatorId, actuatorType,
                                logEntity.getCommandValue(), logEntity.getExecutedAt()));
                    }
                },
                // 매핑 안 되는 키는 로그만 남기지 않고 넘어감
                () -> log.info("알 수 없는 제어 명령키 - 실행 로그 남기지 못함: {}", key)
        ));
    }

    @Override
    public Page<ActuatorRunLogResponse> getRunLogsByActuatorId(Long actuatorId, Pageable pageable) {
        return actuatorRunLogRepository.findByActuatorActuatorIdOrderByExecutedAtDesc(actuatorId, pageable)
                .map(ActuatorRunLogResponse::from);
    }


    @Override
    public List<ActuatorRunLogInternalResponse> getRunLogsForReport(List<Long> locationIds,
                                                                    OffsetDateTime from, OffsetDateTime to) {
        return actuatorRunLogRepository.findForReport(locationIds, from, to).stream()
                .map(ActuatorRunLogInternalResponse::from)
                .toList();
    }

}