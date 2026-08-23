package com.insighton.core.actuator.policy;

import com.insighton.core.domain.actuators.policy.CommandValueRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CommandValueRuleTest {

    @Test
    @DisplayName("AllowedValues - 목록에 있는 값(대소문자 무관)이면 true")
    void 고정값_허용값_true() {
        CommandValueRule rule = new CommandValueRule.AllowedValues(Set.of("ON", "OFF"));

        assertThat(rule.isValid("ON")).isTrue();
        assertThat(rule.isValid("off")).isTrue();
    }

    @Test
    @DisplayName("AllowedValues - 목록에 없는 값이거나 null이면 false")
    void 고정값_비허용값_false() {
        CommandValueRule rule = new CommandValueRule.AllowedValues(Set.of("ON", "OFF"));

        assertThat(rule.isValid("PAUSE")).isFalse();
        assertThat(rule.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("NumericRange - 범위 안의 숫자면 true (경계값 포함)")
    void 숫자범위_범위안_true() {
        CommandValueRule rule = new CommandValueRule.NumericRange(18, 30);

        assertThat(rule.isValid("18")).isTrue();
        assertThat(rule.isValid("30")).isTrue();
        assertThat(rule.isValid("24.5")).isTrue();
    }

    @Test
    @DisplayName("NumericRange - 범위 밖의 숫자면 false")
    void 숫자범위_범위밖_false() {
        CommandValueRule rule = new CommandValueRule.NumericRange(18, 30);

        assertThat(rule.isValid("17.9")).isFalse();
        assertThat(rule.isValid("30.1")).isFalse();
    }

    @Test
    @DisplayName("NumericRange - 숫자로 파싱 안 되는 값이나 null이면 false (예외 대신 false)")
    void 숫자범위_파싱불가_false() {
        CommandValueRule rule = new CommandValueRule.NumericRange(18, 30);

        assertThat(rule.isValid("스물넷")).isFalse();
        assertThat(rule.isValid(null)).isFalse();
    }
}
