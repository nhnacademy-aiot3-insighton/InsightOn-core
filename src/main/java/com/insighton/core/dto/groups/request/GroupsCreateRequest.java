package com.insighton.core.dto.groups.request;


import org.jetbrains.annotations.NotNull;

/**
 * 그룹 생성 시
 * @param name 회사 가입 기관 또는 계열사 공식 명칭
 * @param description 해당 회사 및 사업장에 대한 세부 상세 설명
 * @param location 회사 위치
 */
public record GroupsCreateRequest(
        @NotNull String name,
        String description,
        @NotNull String location
) {
}
