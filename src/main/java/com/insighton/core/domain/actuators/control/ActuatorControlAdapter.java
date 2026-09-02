package com.insighton.core.domain.actuators.control;

import java.util.List;

// 공급자별 제어 Adapter가 구현해야 하는 공통 계약
public interface ActuatorControlAdapter {

    ControlProvider supports();

    ActuatorControlResult control(ActuatorControlCommand command);

    // 이 공급자 계정에 연결된 장치 목록 (액추에이터 등록 시 externalDeviceId 선택용)
    List<ProviderDevice> listDevices();
}
