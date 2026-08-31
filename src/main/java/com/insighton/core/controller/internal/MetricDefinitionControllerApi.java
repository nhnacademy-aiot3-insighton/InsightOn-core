package com.insighton.core.controller.internal;

import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionCreateRequest;
import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;

// Swagger 문서화 전용 인터페이스
@Tag(name = "MetricDefinition (internal)", description = "메트릭 정의(속성 카탈로그) 내부 API")
public interface MetricDefinitionControllerApi {

    @Operation(summary = "메트릭 정의 전체 조회")
    ResponseEntity<List<MetricDefinitionResponse>> getAllMetricDefinitions();

    @Operation(summary = "메트릭 정의 생성")
    ResponseEntity<Void> createMetricDefinition(@Valid MetricDefinitionCreateRequest request);
}
