package com.insighton.core.domain.location.dto.response;

import com.insighton.core.domain.location.entity.Location;
import lombok.Builder;

/**
 * location list 조회 응답
 *
 * @param locationId      location들의 ID
 * @param locationName    실제 근무 및 관제 공간 명칭 (ex. 4층 개발팀 3층 대회의실)
 * @param autoControlMode AI가 제안 or AI가 직접 제어  ENUM(SUGGESTION/AI_DIRECT)
 */
@Builder
public record LocationListResponse(
        Long locationId,
        String locationName,
        Location.AutoControlMode autoControlMode
        // 생성일자 추가
) {
}
