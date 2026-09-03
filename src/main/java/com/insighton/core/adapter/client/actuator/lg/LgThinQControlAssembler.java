package com.insighton.core.adapter.client.actuator.lg;

import com.insighton.core.domain.actuators.control.ActuatorControlCommand;
import com.insighton.core.domain.actuators.control.NeutralCommand;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 공급자 독립 공통 명령(ActuatorControlCommand) -> LG ThinQ Connect control payload.
// payload는 resource로 중첩된 property bag: { "<resource>": { "<property>": <value> }, ... }
// 명령별 resource/property 매핑은 LgThinQVocab enum이 담당한다.
// 실제: POST https://api-kic.lgthinq.com/devices/{id}/control  (docs/provider-contract.md §5)
@Component
public class LgThinQControlAssembler {

    private static final String TEMPERATURE_RESOURCE = "temperature";
    private static final String TEMPERATURE_UNIT = "C";

    // Vocab enum으로 처리하는 명령들. TEMPERATURE 는 값이 동적이라 아래에서 별도 처리.
    private static final List<NeutralCommand> VOCAB_COMMANDS =
            List.of(NeutralCommand.POWER, NeutralCommand.MODE, NeutralCommand.WIND_DIRECTION);

    // desiredState의 각 중립 키를 Vocab에서 찾아 resource -> { property -> value } 로 조립 (변경하는 resource만)
    public Map<String, Object> assemble(ActuatorControlCommand command) {
        ActuatorType type = command.actuatorType();
        Map<String, Object> state = command.desiredState();
        Map<String, Object> payload = new LinkedHashMap<>();

        for (NeutralCommand nc : VOCAB_COMMANDS) {
            if (state.containsKey(nc.stateKey())) {
                putVocab(payload, type, nc, state.get(nc.stateKey()));
            }
        }
        // 온도는 값이 동적이라 Vocab이 아니라 여기서 직접 temperature resource로 ({targetTemperature, unit:"C"})
        if (state.containsKey(NeutralCommand.TEMPERATURE.stateKey())) {
            if (type != ActuatorType.AIRCON) {
                throw new LgThinQApiException(
                        "LG ThinQ 어댑터는 temperature를 AIRCON에서만 지원합니다 (actuatorType=" + type + ")");
            }
            Map<String, Object> temp = new LinkedHashMap<>();
            temp.put("targetTemperature", toTargetTemperature(state.get(NeutralCommand.TEMPERATURE.stateKey())));
            temp.put("unit", TEMPERATURE_UNIT);
            payload.put(TEMPERATURE_RESOURCE, temp);
        }

        if (payload.isEmpty()) {
            throw new LgThinQApiException("LG ThinQ로 변환할 수 있는 명령이 없습니다: " + state);
        }
        return payload;
    }

    // 중립 (명령, 값) 을 Vocab에서 찾아 resource.property 로 얹는다 (같은 resource면 병합)
    @SuppressWarnings("unchecked")
    private void putVocab(Map<String, Object> payload, ActuatorType type, NeutralCommand command, Object neutralValue) {
        LgThinQVocab v = LgThinQVocab.find(type, command, String.valueOf(neutralValue))
                .orElseThrow(() -> new LgThinQApiException(
                        "LG ThinQ가 지원하지 않는 " + command + " 값입니다: " + neutralValue
                                + " (actuatorType=" + type + ")"));
        Map<String, Object> resource = (Map<String, Object>) payload.computeIfAbsent(
                v.resource(), k -> new LinkedHashMap<String, Object>());
        resource.put(v.property(), v.lgValue());
    }

    // 섭씨 정수로 반올림 (LG targetTemperature는 정수)
    private Integer toTargetTemperature(Object temperature) {
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(temperature)));
        } catch (NumberFormatException e) {
            throw new LgThinQApiException("temperature는 숫자여야 합니다: " + temperature);
        }
    }
}
