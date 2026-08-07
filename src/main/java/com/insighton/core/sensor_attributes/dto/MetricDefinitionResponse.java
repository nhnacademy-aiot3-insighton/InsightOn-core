package com.insighton.core.sensor_attributes.dto;


import com.insighton.core.sensor_attributes.entity.MetricDefinition;

/**
 *  시스템 내 지원하는 메트릭 표준 정의 정보를 프론트엔드로 전달하는 DTO
 */
public record MetricDefinitionResponse(
        String metricKey,
        String metricName,
        String unit
) {
    public static MetricDefinitionResponse from(MetricDefinition definition){
        return new MetricDefinitionResponse(
                definition.getMetricKey(),
                definition.getMetricName(),
                definition.getUnit()
        );
    }
}
