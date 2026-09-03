package com.insighton.core.adapter.client.actuator.lg;

import com.insighton.core.adapter.client.actuator.lg.dto.LgThinQControlResponse;
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

// LG_THINQ 공급자용 Adapter. Registry가 이 빈을 supports() 기준으로 자동 편입한다.
@Component
@RequiredArgsConstructor
@Slf4j
public class LgThinQActuatorAdapter implements ActuatorControlAdapter {

    private final LgThinQControlAssembler assembler;
    private final LgThinQApiClient apiClient;
    private final ObjectMapper objectMapper;

    // 담당 공급자 = LG_THINQ
    @Override
    public ControlProvider supports() {
        return ControlProvider.LG_THINQ;
    }

    // 중립 명령 → payload 조립 → 전송 → error 없으면 desiredState를 적용 결과로 반환
    @Override
    public ActuatorControlResult control(ActuatorControlCommand command) {
        Map<String, Object> payload = assembler.assemble(command);
        log.info("[LG ThinQ] {} → {}", command.externalDeviceId(), toJson(payload));
        LgThinQControlResponse response = apiClient.control(command.externalDeviceId(), payload);

        // 응답 본문이 없으면 수락 여부를 알 수 없으므로 실패로 본다 (SmartThings 어댑터와 동일)
        if (response == null) {
            throw new LgThinQApiException("LG ThinQ 응답이 비어 있습니다");
        }
        if (response.error() != null) {
            throw new LgThinQApiException("LG ThinQ가 명령을 거부했습니다: " + response.error());
        }

        // 공급자가 명령을 수락했으므로 요청한 desiredState가 그대로 적용됐다고 보고 CORE에 반영한다.
        return new ActuatorControlResult(command.desiredState(), "messageId=" + response.messageId());
    }

    // 이 종류가 LG ThinQ에서 지원하는 SELECT 명령값 (Front 칩 렌더용)
    @Override
    public Map<String, List<String>> supportedValues(ActuatorType actuatorType) {
        return LgThinQVocab.supportedValues(actuatorType);
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
