package com.insighton.core.influx;

import com.insighton.core.actuator_run_logs.entity.CommandType;
import com.insighton.core.actuators.entity.Actuator;
import com.insighton.core.actuators.repository.ActuatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

// 매 정각, 전환 이벤트 유무와 무관하게 모든 액추에이터의 현재 상태를 재기록
// (AI 배치가 range(start, stop) 구간을 조회할 때 포인트가 0개인 구간이 없도록 보장)
@Slf4j
@Component
@RequiredArgsConstructor
public class ActuatorStatusHeartbeatScheduler {

    private final ActuatorRepository actuatorsRepository;
    private final ActuatorStatusInfluxWriter influxWriter;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void writeHourlyHeartbeat() {
        Instant hourMark = Instant.now();
        List<Actuator> actuators = actuatorsRepository.findAll();

        actuators.forEach(actuator -> {
            // 트랜잭션이 살아있는 이 스레드 안에서 지연로딩 값을 미리 다 꺼냄
            String groupId = String.valueOf(actuator.getLocationId().getGroups().getGroupId());
            String locationId = String.valueOf(actuator.getLocationId().getLocationId());
            String actuatorId = String.valueOf(actuator.getActuatorId());
            String actuatorType = actuator.getActuatorType().name();

            Object powerValue = actuator.getCurrentState() == null ? null
                    : actuator.getCurrentState().get(CommandType.POWER_STATUS.getStateKey());

            // 이제 순수 값들만 비동기 스레드로 넘김 - 지연로딩 걱정 없음
            influxWriter.writeHeartbeat(groupId, locationId, actuatorId, actuatorType,
                    powerValue == null ? null : String.valueOf(powerValue), hourMark);
        });
    }
}