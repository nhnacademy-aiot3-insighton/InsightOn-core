package com.insighton.core.location.exception;

public class EmptyValueException extends RuntimeException {
    public EmptyValueException() {
        super("필수 입력 값이 비어있습니다.");
    }
}
