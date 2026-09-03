package com.insighton.core.domain.actuators.control;

import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.exception.UnsupportedControlProviderException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// (공급자, 종류) 조합이 지원하는 SELECT형 명령값을 계산한다. key=stateKey(mode/windDirection), value=중립값 목록.
// Front 조작 UI가 이 목록대로 칩을 그린다 (Front에는 공급자 분기 없음).
//   - 공급자 연결됨: 해당 어댑터의 supportedValues (SmartThingsVocab / LgThinQVocab)
//   - 미연결(UNBOUND): 중립 기본 목록
@Component
@RequiredArgsConstructor
public class ProviderCommandCatalog {

    private final ActuatorControlAdapterRegistry adapterRegistry;

    // 공급자 미연결일 때 보여줄 중립값 (순서 고정)
    private static final Map<ActuatorType, Map<String, List<String>>> NEUTRAL = Map.of(
            ActuatorType.AIRCON, Map.of(
                    "mode", List.of("COOL", "DRY", "FAN", "AUTO"),
                    "windDirection", List.of("FIXED", "SWING")),
            ActuatorType.AIR_PURIFIER, Map.of("mode", List.of("AUTO", "SLEEP", "TURBO")),
            ActuatorType.VENTILATION_FAN, Map.of("mode", List.of("LOW", "MID", "HIGH")));

    // 공급자 연결됐으면 그 어댑터의 지원값, 미연결이거나 어댑터가 빈값이면 중립 기본값
    public Map<String, List<String>> supportedValues(ControlProvider provider, ActuatorType actuatorType) {
        if (provider == null) {
            return NEUTRAL.getOrDefault(actuatorType, Map.of());
        }
        try {
            Map<String, List<String>> values = adapterRegistry.get(provider).supportedValues(actuatorType);
            return values.isEmpty() ? NEUTRAL.getOrDefault(actuatorType, Map.of()) : values;
        } catch (UnsupportedControlProviderException e) {
            return NEUTRAL.getOrDefault(actuatorType, Map.of());
        }
    }
}
