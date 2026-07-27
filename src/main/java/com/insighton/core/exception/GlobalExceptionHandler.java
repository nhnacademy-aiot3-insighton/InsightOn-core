package com.insighton.core.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

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
                // errorCode.getMessage()로 대체되어 항상 기본 메시지만 반환
                // e.getMessage()를 사용해야 동적 메시지가 실제 API 응답에 포함
                e.getMessage()
        );

        // ErrorCode 내부에 정의된 httpStatus(예: 404 NOT_FOUND)로 응답 상태 설정
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }


    /**
     * @Valid 또는 @Validated 검증 실패 예외 처리 (RequestBody)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 첫 번째 검증 실패 메시지를 가져오거나 기본 메시지 사용
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> String.format("[%s] %s", fieldError.getField(), fieldError.getDefaultMessage()))
                .orElse(ErrorCode.INVALID_INPUT_VALUE.getMessage());

        ErrorResponse response = new ErrorResponse(
                ErrorCode.INVALID_INPUT_VALUE.getCode(),
                errorMessage
        );

        return new ResponseEntity<>(response, ErrorCode.INVALID_INPUT_VALUE.getHttpStatus());
    }

    /**
     * 메서드 파라미터 레벨 검증 실패 예외 처리 (PathVariable, RequestParam 등)
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        ErrorResponse response = new ErrorResponse(
                ErrorCode.INVALID_INPUT_VALUE.getCode(),
                ErrorCode.INVALID_INPUT_VALUE.getMessage()
        );

        return new ResponseEntity<>(response, ErrorCode.INVALID_INPUT_VALUE.getHttpStatus());
    }

    // 예외 응답 표준 DTO
    public record ErrorResponse(String code, String message) {}
}