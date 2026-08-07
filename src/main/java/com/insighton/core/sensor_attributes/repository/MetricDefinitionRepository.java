package com.insighton.core.sensor_attributes.repository;

import com.insighton.core.sensor_attributes.entity.MetricDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MetricDefinitionRepository extends JpaRepository<MetricDefinition, String> {

    // 기존 enum때 equalsIgnoreCase 비겨 동작을 그대로 유지하기 위해 대소문자 무시 조회
    @Query("SELECT m FROM MetricDefinition m WHERE lower(m.metricKey) = lower(:metricKey) " )
    Optional<MetricDefinition> findByMetricKeyIgnoreCase(@Param("metricKey") String metricKey);

    
}
