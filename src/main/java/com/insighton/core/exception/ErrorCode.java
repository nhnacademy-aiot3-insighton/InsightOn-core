
package com.insighton.core.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 404 Not Found
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "DEVICE_404", "해당 기기를 찾을 수 없습니다."),
    ACTUATOR_NOT_FOUND(HttpStatus.NOT_FOUND, "ACTUATOR_404", "액추에이터를 찾을 수 없습니다."),
    METRIC_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "METRIC_404", "정의되지 않거나 존재하지 않는 메트릭 키입니다."),


    // 400 Bad Request
    NO_ACTUATOR_STATE(HttpStatus.NOT_FOUND, "DEVICE_400", "액추에이터 상태 값이 없습니다"),
    NO_NEW_LOCATION_ID(HttpStatus.NOT_FOUND, "LOCATION_ID_400", "새위치 정보가 없습니다"),
    NO_NEW_DEVICE_NAME(HttpStatus.NOT_FOUND, "DEVICE_NAME_400", "새디바이스 이름이 없습니다"),
    NO_NEW_ACTUATOR_VALUE(HttpStatus.NOT_FOUND, "ACTUATOR_VALUE_400", "새로운 값이 없습니다"),
    NO_NEW_ACTUATOR_STATE(HttpStatus.NOT_FOUND, "ACTUATOR_STATE_400", "새로운 상태값이 없습니다"),
    NO_NEW_ACTUATOR_NAME(HttpStatus.NOT_FOUND, "ACTUCATOR_NAME_400", "새로운 이름이 없습니다"),
    DUPLICATE_DEVICE_EUI(HttpStatus.BAD_REQUEST, "DEVICE_EUI_400","중복된 값이 있습니다"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청 값입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}