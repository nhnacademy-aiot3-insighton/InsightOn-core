package com.insighton.core.actuatorrunlogs.entity;

import java.util.Set;

// 명령 하나의 허용 값 규칙 - 고정값 목록(ON/OFF 등) 또는 숫자 범위(온도) 둘 중 하나
/*
    sealed가 하는일 이 인터페이스를 구현할 수 있는 건 AllowedValues, NumericRange 이거 두개뿐이고,
    다른 어디서 implements CommandValueRule을 새로 만들려고하면 컴파일 에러가남
    또는 sealed 선언에 permits로 명시적으로 허용해줘야 가능함
 */

public sealed interface CommandValueRule {

    boolean isValid(String value);

    record AllowedValues(Set<String> values) implements CommandValueRule {
        @Override
        public boolean isValid(String value) {
            return value != null && values.contains(value.toUpperCase());
        }
    }

    record NumericRange(double min, double max) implements CommandValueRule {
        @Override
        public boolean isValid(String value) {
            try {
                double parsed = Double.parseDouble(value);
                return parsed >= min && parsed <= max;
            } catch (NumberFormatException | NullPointerException e) {
                return false;
            }
        }
    }
}