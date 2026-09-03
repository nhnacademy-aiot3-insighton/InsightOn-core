package com.insighton.core.adapter.client.actuator.smartthings;

import com.insighton.core.adapter.client.actuator.smartthings.dto.SmartThingsCommandRequest;
import com.insighton.core.adapter.client.actuator.smartthings.dto.SmartThingsCommandResponse;
import com.insighton.core.domain.actuators.control.ActuatorControlAdapter;
import com.insighton.core.domain.actuators.control.ActuatorControlCommand;
import com.insighton.core.domain.actuators.control.ActuatorControlResult;
import com.insighton.core.domain.actuators.control.ControlProvider;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// SMART_THINGS 공급자용 Adapter. Registry가 이 빈을 supports() 기준으로 자동 편입한다.
@Component
@RequiredArgsConstructor
@Slf4j
public class SmartThingsActuatorAdapter implements ActuatorControlAdapter {

    private static final String ACCEPTED = "ACCEPTED";

    private final SmartThingsCommandAssembler assembler;
    private final SmartThingsApiClient apiClient;
    private final ObjectMapper objectMapper;

    // 담당 공급자 = SMART_THINGS
    @Override
    public ControlProvider supports() {
        return ControlProvider.SMART_THINGS;
    }

    // 중립 명령 → 요청 조립 → 전송 → 전건 ACCEPTED 확인 후 desiredState를 적용 결과로 반환
    @Override
    public ActuatorControlResult control(ActuatorControlCommand command) {
        SmartThingsCommandRequest request = assembler.assemble(command);
        log.info("[SmartThings] {} → {}", command.externalDeviceId(), toJson(request));
        SmartThingsCommandResponse response = apiClient.sendCommands(command.externalDeviceId(), request);

        if (response == null || response.results() == null || response.results().isEmpty()) {
            throw new SmartThingsApiException("SmartThings 응답에 results가 없습니다");
        }
        boolean allAccepted = response.results().stream()
                .allMatch(r -> r.status() != null && ACCEPTED.equalsIgnoreCase(r.status()));
        if (!allAccepted) {
            throw new SmartThingsApiException("SmartThings가 일부 명령을 수락하지 않았습니다: " + response.results());
        }

        // 공급자가 명령을 수락했으므로, 요청한 desiredState가 그대로 적용됐다고 보고 CORE에 반영한다.
        return new ActuatorControlResult(command.desiredState(), summarize(response));
    }

    // 이 종류가 SmartThings에서 지원하는 SELECT 명령값 (Front 칩 렌더용)
    @Override
    public Map<String, List<String>> supportedValues(ActuatorType actuatorType) {
        return SmartThingsVocab.supportedValues(actuatorType);
    }

    // 응답 요약 로그 문자열
    private String summarize(SmartThingsCommandResponse response) {
        return "results=" + response.results().size() + " all " + ACCEPTED;
    }

    // 객체를 로그용 JSON 문자열로 (실패 시 toString)
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
