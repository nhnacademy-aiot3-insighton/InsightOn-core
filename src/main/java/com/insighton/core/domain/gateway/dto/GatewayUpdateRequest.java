package com.insighton.core.domain.gateway.dto;

import com.insighton.core.domain.gateway.entity.ProtocolType;
import java.util.Map;

/**
 * 부분 수정용 — 세 필드 모두 생략 가능하며, 요청에 없는 필드는 기존 값을 유지함.
 * groupsId는 소유권 식별자라 수정 대상에서 제외함 — 바꾸고 싶으면 삭제 후 재생성.
 * protocolType이 바뀌면 connectionConfig도 함께 와야 함 — 프로토콜마다 요구하는 설정 형태가
 * 달라서(MQTT는 brokerUrls, MODBUS_TCP는 다른 키), 옛 config가 새 프로토콜에 그대로 유효할 수 없음.
 */
public record GatewayUpdateRequest(
        String name,
        ProtocolType protocolType,
        Map<String, Object> connectionConfig
) {}
