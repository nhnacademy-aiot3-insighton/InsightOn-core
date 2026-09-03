package com.insighton.core.adapter.client.actuator.smartthings;

import com.insighton.core.adapter.client.actuator.smartthings.dto.SmartThingsCommandRequest;
import com.insighton.core.domain.actuators.control.ActuatorControlCommand;
import com.insighton.core.domain.actuators.control.NeutralCommand;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 공급자 독립 공통 명령(ActuatorControlCommand) -> SmartThings "Execute commands" 요청.
// 명령별 capability 매핑은 SmartThingsVocab enum이 담당한다.
// 실제: POST https://api.smartthings.com/v1/devices/{id}/commands  (docs/provider-contract.md §4)
@Component
public class SmartThingsCommandAssembler {

    private static final String COMPONENT_MAIN = "main";

    // Vocab enum으로 처리하는 명령들 (값이 고정 매핑). TEMPERATURE 는 값이 동적이라 아래에서 별도 처리.
    private static final List<NeutralCommand> VOCAB_COMMANDS =
            List.of(NeutralCommand.POWER, NeutralCommand.MODE, NeutralCommand.WIND_DIRECTION);

    // desiredState의 각 중립 키를 Vocab에서 찾아 SmartThings command 배열로 조립 (해당 키 없으면 건너뜀)
    public SmartThingsCommandRequest assemble(ActuatorControlCommand command) {
        ActuatorType type = command.actuatorType();
        Map<String, Object> state = command.desiredState();
        List<SmartThingsCommandRequest.Command> commands = new ArrayList<>();

        for (NeutralCommand nc : VOCAB_COMMANDS) {
            if (state.containsKey(nc.stateKey())) {
                commands.add(fromVocab(type, nc, state.get(nc.stateKey())));
            }
        }
        // 온도는 값이 동적이라 Vocab이 아니라 여기서 직접 capability로
        if (state.containsKey(NeutralCommand.TEMPERATURE.stateKey())) {
            requireAircon(type, "temperature");
            commands.add(new SmartThingsCommandRequest.Command(COMPONENT_MAIN,
                    "thermostatCoolingSetpoint", "setCoolingSetpoint",
                    List.of(toNumber(state.get(NeutralCommand.TEMPERATURE.stateKey())))));
        }

        if (commands.isEmpty()) {
            throw new SmartThingsApiException("SmartThings로 변환할 수 있는 명령이 없습니다: " + state);
        }
        return new SmartThingsCommandRequest(commands);
    }

    // 중립 (명령, 값) 을 Vocab에서 찾아 capability 명령으로 변환
    private SmartThingsCommandRequest.Command fromVocab(ActuatorType type, NeutralCommand command, Object neutralValue) {
        SmartThingsVocab v = SmartThingsVocab.find(type, command, String.valueOf(neutralValue))
                .orElseThrow(() -> new SmartThingsApiException(
                        "SmartThings가 지원하지 않는 " + command + " 값입니다: " + neutralValue
                                + " (actuatorType=" + type + ")"));
        return new SmartThingsCommandRequest.Command(COMPONENT_MAIN, v.capability(), v.stCommand(), v.arguments());
    }

    // AIRCON 전용 명령을 다른 종류에 보내면 거절
    private void requireAircon(ActuatorType actuatorType, String field) {
        if (actuatorType != ActuatorType.AIRCON) {
            throw new SmartThingsApiException(
                    "SmartThings 어댑터는 " + field + "를 AIRCON에서만 지원합니다 (actuatorType=" + actuatorType + ")");
        }
    }

    // "24" / 24 / 24.0 -> 정수면 int, 아니면 double. 삼항으로 합치면 int가 double로 승격되므로 return 분리.
    private Object toNumber(Object value) {
        try {
            double d = Double.parseDouble(String.valueOf(value));
            if (d == Math.rint(d) && !Double.isInfinite(d)) {
                return (int) d;
            }
            return d;
        } catch (NumberFormatException e) {
            throw new SmartThingsApiException("temperature는 숫자여야 합니다: " + value);
        }
    }
}
