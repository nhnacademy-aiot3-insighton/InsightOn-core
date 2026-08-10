package com.insighton.core.domain.location.dto.response;

import com.insighton.core.domain.location.entity.Location;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * Location 상세 정보 조회 응답
 *
 * @param locationId      상세 조회 중인 location ID
 * @param groupId         구역이 속한 소속 회사 마스터 식별 ID (FK)
 * @param locationName    실제 근무 및 관제 공간 명칭 (ex. 4층 개발팀 3층 대회의실)
 * @param createdAt       해당 구역 정보 최초 생성 및 등록 일시
 * @param autoControlMode AI가 제안 or AI가 직접 제어  ENUM(SUGGESTION/AI_DIRECT)
 */
@Builder
public record LocationResponse(
        Long locationId,
        Long groupId,
        String locationName,
        OffsetDateTime createdAt,
        Location.AutoControlMode autoControlMode
) {
}
