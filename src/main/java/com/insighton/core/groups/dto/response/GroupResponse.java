package com.insighton.core.groups.dto.response;


import com.insighton.core.groups.entity.Groups;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 그룹 조회 시 응답
 * @param groupId 그룹 고유 id
 * @param name 회사 가입 기관 또는 계열사 공식 명칭
 * @param description 해당 회사 및 사업장에 대한 세부 상세 설명
 * @param location 회사 위치
 * @param inviteToken 초대 코드 / 링크
 * @param createdAt SaaS 테넌트 서비스 최초 가입 및 생성 일시
 */
@Builder
public record GroupResponse(
        Long groupId,
        String name,
        String description,
        String location,
        String inviteToken,
        LocalDateTime createdAt
) {

    /**
     * 그룹 관리자 조회 시
     * @param entity
     * @return
     */
    public static GroupResponse ofAdmin(Groups entity){
        return GroupResponse.builder()
                .groupId(entity.getGroupId())
                .name(entity.getName())
                .description(entity.getDescription())
                .location(entity.getLocation())
                .inviteToken(entity.getInviteToken())
                .createdAt(entity.getCreatedAt())
                .build();

    }

    /**
     *  일반 사용자 조회 시 inviteToken null 값으로 가리기
     * @param entity
     * @return
     */
    public static GroupResponse ofPublic(Groups entity){
        return GroupResponse.builder()
                .groupId(entity.getGroupId())
                .name(entity.getName())
                .description(entity.getDescription())
                .location(entity.getLocation())
                .inviteToken(null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

}
