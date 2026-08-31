package com.insighton.core.controller.api;

import com.insighton.core.domain.sensorattributes.dto.SensorAttributeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

// Swagger 문서화 전용 인터페이스
@Tag(name = "SensorAttribute", description = "센서 속성(메트릭) API")
public interface SensorAttributeControllerApi {

    @Operation(summary = "센서 속성 목록 조회")
    ResponseEntity<List<SensorAttributeResponse>> getSensorAttribute(
            @Parameter(description = "사용자 ID") Long userId,
            @Parameter(description = "센서 ID") Long sensorId);

    @Operation(summary = "센서 속성 삭제")
    ResponseEntity<Void> deleteSensorAttribute(
            @Parameter(description = "사용자 ID") Long userId,
            @Parameter(description = "센서 ID") Long sensorId,
            @Parameter(description = "메트릭 키") String metricKey);
}
