package com.insighton.core.widgets.exception;

public class InvalidDateTimeFormatException extends RuntimeException {
    public InvalidDateTimeFormatException(String duration) {
        super("유효하지 않은 시간/기간 형식입니다 : " + duration);
    }
}
