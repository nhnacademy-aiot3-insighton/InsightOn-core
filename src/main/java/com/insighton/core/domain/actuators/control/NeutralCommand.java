package com.insighton.core.domain.actuators.control;

import java.util.Optional;

// CORE·Front가 쓰는 공급자 독립 명령 키. desiredState의 각 키가 이 중 하나로 매핑된다.
// 각 공급자 Vocabulary(SmartThingsVocab / LgThinQVocab)가 이 값을 자기 API 표현으로 번역한다.
public enum NeutralCommand {

    POWER("power"),          // 값: ON / OFF
    MODE("mode"),            // 값: 종류별 (에어컨 COOL/DRY/FAN/AUTO(+LG AIRCLEAN), 공청기 AUTO/SLEEP/TURBO, 환풍기 LOW/MID/HIGH)
    WIND_DIRECTION("windDirection"), // 값: FIXED / SWING (에어컨만, 공급자별 wire 표현 상이)
    TEMPERATURE("temperature"); // 값: 정수 (에어컨만)

    private final String stateKey;

    NeutralCommand(String stateKey) {
        this.stateKey = stateKey;
    }

    public String stateKey() {
        return stateKey;
    }

    // desiredState 키 문자열로 역조회
    public static Optional<NeutralCommand> fromStateKey(String key) {
        for (NeutralCommand c : values()) {
            if (c.stateKey.equals(key)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}
