package com.insighton.core.usecase.actuator;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.actuatorrunlogs.dto.ActuatorRunLogInternalResponse;
import com.insighton.core.domain.actuatorrunlogs.service.ActuatorRunLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

// AI 리포트 배치용 실행 로그 조회 - X-USER-ID가 없는 내부 호출이라 권한 검증 없음
// (내부 배치 호출이라 groupMemberService로 검증할 대상 자체가 없음)
@UseCase
@RequiredArgsConstructor
public class GetActuatorRunLogsForReportUseCase {
    private final ActuatorRunLogService actuatorRunLogService;

    @Transactional(readOnly = true)
    public List<ActuatorRunLogInternalResponse> getRunLogsForReport(List<Long> locationIds, OffsetDateTime from, OffsetDateTime to) {
        return actuatorRunLogService.getRunLogsForReport(locationIds, from, to);
    }
}
