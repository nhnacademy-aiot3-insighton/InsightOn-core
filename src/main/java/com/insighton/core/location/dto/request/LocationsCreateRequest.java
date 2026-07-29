package com.insighton.core.location.dto.request;

import com.insighton.core.location.entity.Locations;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * location 생성 요청
 *
 * @param locationName    실제 근무 및 관제 공간 명칭 (ex. 4층 개발팀 3층 대회의실)
 * @param autoControlMode AI가 제안 or AI가 직접 제어  ENUM(SUGGESTION/AI_DIRECT)
 */
@Builder
public record LocationsCreateRequest(
        @NotBlank String locationName,
        @NotNull Locations.AutoControlMode autoControlMode
) {
}
