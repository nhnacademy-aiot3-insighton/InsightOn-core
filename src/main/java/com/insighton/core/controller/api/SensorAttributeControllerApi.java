package com.insighton.core.controller.api;

import com.insighton.core.domain.sensorattributes.dto.SensorAttributeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

// Swagger 문서화 애노테이션 전용 인터페이스 - 실제 스프링 바인딩 애노테이션은 구현체(SensorAttributeController)에만 남김
@Tag(name = "SensorAttribute", description = "센서 속성(메트릭) 조회/삭제 API")
public interface SensorAttributeControllerApi {

    @Operation(summary = "센서 속성 목록 조회", description = "센서가 갖고 있는 메트릭 속성(예: 온도, 습도) 목록을 조회합니다.")
    ResponseEntity<List<SensorAttributeResponse>> getSensorAttribute(
            @Parameter(description = "요청 사용자 ID", required = true) Long userId,
            @Parameter(description = "센서 ID", required = true) Long sensorId);

    @Operation(summary = "센서 속성 삭제", description = "센서에서 특정 메트릭 키(속성)를 제거합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공")
    })
    ResponseEntity<Void> deleteSensorAttribute(
            @Parameter(description = "요청 사용자 ID", required = true) Long userId,
            @Parameter(description = "센서 ID", required = true) Long sensorId,
            @Parameter(description = "삭제할 메트릭 키 (예: co2)", required = true) String metricKey);
}
