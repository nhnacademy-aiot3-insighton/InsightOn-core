
package com.insighton.core.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 404 Not Found
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "DEVICE_404", "해당 기기를 찾을 수 없습니다."),
    METRIC_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "METRIC_404", "정의되지 않거나 존재하지 않는 메트릭 키입니다."),


    // 400 Bad Request
    DUPLICATE_DEVICE_EUI(HttpStatus.BAD_REQUEST, "EUI_400","중복된 값이있습니다"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청 값입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}