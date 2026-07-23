package com.insighton.core.device.error;

import lombok.Getter;
import org.springframework.web.server.ResponseStatusException;

/**
 * GlobalExceptionHandler 없이도 HTTP 상태 코드를 자동으로 응답해주는 커스텀 예외
 */
@Getter
public class CustomException extends ResponseStatusException {

    private final ErrorCode errorCode;

    // 기본 메시지 사용
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getHttpStatus(), errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 동적 메시지 사용 (예: "1번 기기를 찾을 수 없습니다")
    public CustomException(ErrorCode errorCode, String customMessage) {
        super(errorCode.getHttpStatus(), customMessage);
        this.errorCode = errorCode;
    }
}