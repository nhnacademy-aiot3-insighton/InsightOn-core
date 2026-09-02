package com.insighton.core.adapter.client.actuator.lg;

import com.insighton.core.adapter.client.actuator.lg.dto.LgThinQControlRequest;
import com.insighton.core.adapter.client.actuator.lg.dto.LgThinQControlResponse;
import com.insighton.core.domain.actuators.control.ActuatorControlAdapter;
import com.insighton.core.domain.actuators.control.ActuatorControlCommand;
import com.insighton.core.domain.actuators.control.ActuatorControlResult;
import com.insighton.core.domain.actuators.control.ControlProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// LG_THINQ 공급자용 Adapter. Registry가 이 빈을 supports() 기준으로 자동 편입한다.
@Component
@RequiredArgsConstructor
@Slf4j
public class LgThinQActuatorAdapter implements ActuatorControlAdapter {

    private final LgThinQControlAssembler assembler;
    private final LgThinQApiClient apiClient;
    private final ObjectMapper objectMapper;

    @Override
    public ControlProvider supports() {
        return ControlProvider.LG_THINQ;
    }

    @Override
    public ActuatorControlResult control(ActuatorControlCommand command) {
        LgThinQControlRequest request = assembler.assemble(command);
        log.info("[LG ThinQ] {} → {}", command.externalDeviceId(), toJson(request));
        LgThinQControlResponse response = apiClient.control(command.externalDeviceId(), request);

        if (response != null && response.error() != null) {
            throw new LgThinQApiException("LG ThinQ가 명령을 거부했습니다: " + response.error());
        }

        // 공급자가 명령을 수락했으므로 요청한 desiredState가 그대로 적용됐다고 보고 CORE에 반영한다.
        return new ActuatorControlResult(command.desiredState(),
                response == null ? null : ("messageId=" + response.messageId()));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
