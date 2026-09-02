package com.insighton.core.actuator.control;

import com.insighton.core.domain.actuators.control.ControlProvider;
import com.insighton.core.domain.actuators.control.ExternalDeviceIdGenerator;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalDeviceIdGeneratorTest {

    @Test
    @DisplayName("공급자/종류 접두사 + 랜덤 8자 형식으로 생성한다")
    void generate_형식() {
        String stAircon = ExternalDeviceIdGenerator.generate(ControlProvider.SMART_THINGS, ActuatorType.AIRCON);
        String lgPurifier = ExternalDeviceIdGenerator.generate(ControlProvider.LG_THINQ, ActuatorType.AIR_PURIFIER);
        String lgFan = ExternalDeviceIdGenerator.generate(ControlProvider.LG_THINQ, ActuatorType.VENTILATION_FAN);

        assertThat(stAircon).matches("st-aircon-[0-9a-f]{8}");
        assertThat(lgPurifier).matches("lg-purifier-[0-9a-f]{8}");
        assertThat(lgFan).matches("lg-fan-[0-9a-f]{8}");
    }

    @Test
    @DisplayName("호출마다 다른 값이 나온다")
    void generate_유니크() {
        String a = ExternalDeviceIdGenerator.generate(ControlProvider.SMART_THINGS, ActuatorType.AIRCON);
        String b = ExternalDeviceIdGenerator.generate(ControlProvider.SMART_THINGS, ActuatorType.AIRCON);

        assertThat(a).isNotEqualTo(b);
    }
}
