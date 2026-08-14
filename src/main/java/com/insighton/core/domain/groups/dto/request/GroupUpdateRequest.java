package com.insighton.core.domain.groups.dto.request;

/**
 * 그룹 수정 시
 *
 * @param name        회사 가입 기관 또는 계열사 공식 명칭
 * @param description 해당 회사 및 사업장에 대한 세부 상세 설명
 * @param groupRegion 회사 위치
 */
public record GroupUpdateRequest(
        String name,
        String description,
        String groupRegion
) {
}
