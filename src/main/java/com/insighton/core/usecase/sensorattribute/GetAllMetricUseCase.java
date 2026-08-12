package com.insighton.core.usecase.sensorattribute;


import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionResponse;
import com.insighton.core.domain.sensorattributes.service.SensorAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class GetAllMetricUseCase {
    private final SensorAttributeService sensorAttributeService;

    @Transactional(readOnly = true)
    public List<MetricDefinitionResponse> getAllMetric(){
        return sensorAttributeService.getAllMetricDefinitions();
    }
}
