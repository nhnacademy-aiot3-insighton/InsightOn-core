package com.insighton.core.domain.actuators.event;

// 액추에이터 단독 삭제(그룹/장소 삭제에 딸려온 게 아닌 경우) 시 룰엔진에 알리기 위한 이벤트
public record ActuatorDeletedEvent(Long actuatorId) {
}
