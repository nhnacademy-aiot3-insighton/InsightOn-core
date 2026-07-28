package com.insighton.core.gateway.dto;

import com.insighton.core.gateway.entity.ProtocolType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * connection_config 내부 필수 키(broker URL/인증정보 등) 검증은 Service 레이어에서 처리(Issue01 체크리스트 참고).
 * 여기서는 요청 자체의 필수 최상위 필드만 검증함.
 */
public record GatewayCreateRequest(
        @NotNull Long groupsId,
        @NotBlank String name,
        @NotNull ProtocolType protocolType,
        @NotNull Map<String, Object> connectionConfig
) {}
