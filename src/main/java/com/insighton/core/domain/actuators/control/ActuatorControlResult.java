package com.insighton.core.domain.actuators.control;

import java.util.Map;

// Adapter가 공급자 API 성공 응답을 처리한 결과 - Facade가 이 값을 그대로 CORE 상태에 반영
public record ActuatorControlResult(
        Map<String, Object> appliedState,
        String providerRawResponse // 실행 로그/디버깅용 원본 응답 (필요 없으면 null로 둬도 됨)
) {
}
