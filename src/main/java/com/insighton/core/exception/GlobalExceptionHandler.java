package com.insighton.core.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Controller에서 발생하는 예외를 일관된 HTTP 상태 코드와 응답으로 변환하는 전역 예외 처리기
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();

        ErrorResponse response = new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage()
        );

        // ErrorCode 내부에 정의된 httpStatus(예: 404 NOT_FOUND)로 응답 상태 설정
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }

    // 예외 응답 표준 DTO
    public record ErrorResponse(String code, String message) {}
}