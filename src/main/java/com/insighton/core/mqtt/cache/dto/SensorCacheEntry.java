package com.insighton.core.mqtt.cache.dto;

/**
 * devEui 조회 결과를 캐시(Caffeine/Redis)에 담기 위한 최소 필드 집합.
 * 패킷 라우팅 판단에 실제로 쓰이는 값만 포함하며, name/type/created_at처럼 핫패스에 안 쓰이는
 * 표시용 필드는 의도적으로 제외함.
 *
 * @param sensorId   기기 PK (DB에서 발급된 값)
 * @param sensorEui  MQTT 패킷에 실려오는 기기 고유 식별자, 캐시 키
 * @param gatewayId  소속 게이트웨이 PK, group_id 매핑에 사용됨
 * @param locationId 배치된 공간 PK, 미배치 상태면 null
 */
public record SensorCacheEntry(
        Long sensorId,
        String sensorEui,
        Long gatewayId,
        Long locationId
) {
}