package com.insighton.core.controller.api;

import com.insighton.core.controller.swagger.SensorAttributeControllerApi;
import com.insighton.core.domain.sensorattributes.dto.SensorAttributeResponse;
import com.insighton.core.usecase.sensorattribute.DeleteAttributeUseCase;
import com.insighton.core.usecase.sensorattribute.GetAllAttributeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sensor/{sensor-id}/attribute")
@RequiredArgsConstructor
public class SensorAttributeController implements SensorAttributeControllerApi {

    private final GetAllAttributeUseCase getAllAttributeUseCase;
    private final DeleteAttributeUseCase deleteAttributeUseCase;

    @Override
    @GetMapping
    public ResponseEntity<List<SensorAttributeResponse>> getSensorAttribute(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("sensor-id") Long sensorId){
        return ResponseEntity.ok(getAllAttributeUseCase.getAllAttributeBySensorId(userId, sensorId));
    }

    @Override
    @DeleteMapping("/{metric-key}")
    public ResponseEntity<Void> deleteSensorAttribute(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("sensor-id") Long sensorId,
            @PathVariable("metric-key") String metricKey){
        deleteAttributeUseCase.deleteAttribute(userId, sensorId, metricKey);
        return ResponseEntity.noContent().build();
    }
}