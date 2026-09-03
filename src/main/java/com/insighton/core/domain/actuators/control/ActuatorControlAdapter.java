package com.insighton.core.domain.actuators.control;

import com.insighton.core.domain.actuators.entity.ActuatorType;

import java.util.List;
import java.util.Map;

// 공급자별 제어 Adapter가 구현해야 하는 공통 계약
public interface ActuatorControlAdapter {

    // 이 Adapter가 담당하는 공급자 (Registry가 이 값으로 빈을 분류)
    ControlProvider supports();

    // 중립 명령을 공급자 API 요청으로 바꿔 보내고, 적용된 상태를 결과로 돌려준다
    ActuatorControlResult control(ActuatorControlCommand command);

    // 이 공급자가 해당 종류에서 지원하는 SELECT형 명령값. key=stateKey(mode/windDirection), value=중립값 목록.
    // 공급자마다 지원 값이 달라(에어컨 냉방/제습은 공통, 공기청정은 LG만 등) Front가 이 목록대로 칩을 그린다.
    Map<String, List<String>> supportedValues(ActuatorType actuatorType);
}
