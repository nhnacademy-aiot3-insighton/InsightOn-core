package com.insighton.core.sensor_attributes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 새 메트릭 종류(센서 온보딩 시) 등록 요청 - ex. {"metricKey":"pm2.5", "metricName":"미세먼지", "unit":"㎍/㎥"}
public record MetricDefinitionCreateRequest(
        @NotBlank(message = "metricKey는 필수입니다")
        @Size(max = 50, message = "metricKey는 50자를 넘을 수 없습니다")
        String metricKey,

        @NotBlank(message = "metricName은 필수입니다")
        @Size(max = 100, message = "metricName은 100자를 넘을 수 없습니다")
        String metricName,

        @Size(max = 20, message = "unit은 20자를 넘을 수 없습니다")
        String unit // 단위 없는 메트릭도 있어서(전원상태 등) 필수 아님
) {}
