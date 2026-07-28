package com.insighton.core.groups.dto.response;

import lombok.Builder;

/**
 * 시스템 관리자의 group List 조회용
 *
 * @param groupId     그룹 id
 * @param name        회사 가입 기관 또는 계열사 공식 명칭
 * @param description 해당 회사 및 사업장에 대한 세부 상세 설명 (nullable 가능)
 * @param groupRegion 회사 위치 (지번)
 */
@Builder
public record GroupsAdminResponse(
        Long groupId,
        String name,
        String description,
        String groupRegion
) {
}
