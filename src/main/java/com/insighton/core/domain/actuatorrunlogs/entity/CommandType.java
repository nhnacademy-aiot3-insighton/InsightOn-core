package com.insighton.core.domain.actuatorrunlogs.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

// currentState(JSON) 안에서 실제 사용하는 키(stateKey)와 매핑됨
// MEtricDefinition과 같은 패턴 - 실제 프론트/AI가 보내는 키 이름에 맞게 stateKey 값 조정 필요
@Getter
@RequiredArgsConstructor
public enum CommandType {
    POWER_STATUS("power"),
    OPERATION_MODE("mode"),
    SET_TEMPERATURE("temperature");

    private final String stateKey;

    public static Optional<CommandType> fromStateKey(String key){
        if(key == null){
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(c -> c.stateKey.equalsIgnoreCase(key))
                .findFirst();

    }
}
