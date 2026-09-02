package com.insighton.core.domain.actuators.control;

// 공급자별 제어 Adapter가 구현해야 하는 공통 계약
public interface ActuatorControlAdapter {

    ControlProvider supports();

    ActuatorControlResult control(ActuatorControlCommand command);
}
