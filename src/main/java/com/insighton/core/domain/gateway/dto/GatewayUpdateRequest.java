package com.insighton.core.domain.gateway.dto;

import com.insighton.core.domain.gateway.entity.ProtocolType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * groupsId는 소유권 식별자라 수정 대상에서 제외함 — 바꾸고 싶으면 삭제 후 재생성.
 */
public record GatewayUpdateRequest(
        @NotBlank String name,
        @NotNull ProtocolType protocolType,
        @NotNull Map<String, Object> connectionConfig
) {}
