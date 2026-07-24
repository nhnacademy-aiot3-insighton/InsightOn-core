package com.insighton.core.location.dto.response;

import com.insighton.core.location.entity.Locations;
import lombok.Builder;

/**
 * location list 조회 응답
 *
 * @param locationName    실제 근무 및 관제 공간 명칭 (ex. 4층 개발팀 3층 대회의실)
 * @param autoControlMode AI가 제안 or AI가 직접 제어  ENUM(SUGGESTION/AI_DIRECT)
 */
@Builder
public record LocationsListResponse(
        String locationName,
        Locations.AutoControlMode autoControlMode
) {
}
