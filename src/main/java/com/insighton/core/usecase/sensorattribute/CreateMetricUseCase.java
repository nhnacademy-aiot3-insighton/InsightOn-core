package com.insighton.core.usecase.sensorattribute;


import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionCreateRequest;
import com.insighton.core.domain.sensorattributes.service.SensorAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateMetricUseCase {
    private final SensorAttributeService sensorAttributeService;

    @Transactional
    public void createMetric(MetricDefinitionCreateRequest request){
        sensorAttributeService.createMetricDefinition(request);
    }
}
