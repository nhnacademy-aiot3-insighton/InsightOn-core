package com.insighton.core.domain.sensors.event;

// 센서 단독 삭제(그룹 삭제에 딸려온 게 아닌 경우) 시 룰엔진에 알리기 위한 이벤트
public record SensorDeletedEvent(Long sensorId) {
}
