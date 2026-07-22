
package com.insighton.core.deviceAttribute.error;

import lombok.Getter;


/**
 * 도메인/비즈니스 로직 처리 중 발생하는 통합 예외 클래스.
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    // 기본 에러 메시지 사용
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 동적 메시지가 필요한 경우 (예: "1번 기기를 찾을 수 없습니다")
    public CustomException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}