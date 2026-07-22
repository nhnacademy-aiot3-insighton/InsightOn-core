package com.insighton.core.exception;

import com.insighton.core.gateway.exception.GatewayAccessDeniedException;
import com.insighton.core.gateway.exception.GatewayNotFoundException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 도메인이 늘어나면 이 클래스에 핸들러 메서드 추가.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 게이트웨이를 찾을 수 없는 예외를 HTTP 404 응답으로 변환한다.
     *
     * @return HTTP 404 상태와 예외 메시지를 담은 오류 응답
     */
    @ExceptionHandler(GatewayNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(GatewayNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), e.getMessage()));
    }

    /**
     * 접근이 거부된 예외를 HTTP 403 응답으로 변환합니다.
     *
     * @param e 접근 거부 예외
     * @return HTTP 403 상태와 예외 메시지를 담은 오류 응답
     */
    @ExceptionHandler(GatewayAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(GatewayAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), e.getMessage()));
    }

    /**
     * 잘못된 요청으로 발생한 예외를 처리한다.
     *
     * @param e 잘못된 요청의 원인을 나타내는 예외
     * @return HTTP 400 상태와 예외 메시지를 담은 오류 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }

    /**
     * 요청 필드의 유효성 검증 오류를 HTTP 400 응답으로 변환한다.
     *
     * @param e 유효성 검증에 실패한 요청의 예외 정보
     * @return 필드별 검증 오류 메시지를 포함한 HTTP 400 오류 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message));
    }
}
