package com.insighton.core.domain.sensorattributes.repository;

import com.insighton.core.domain.sensorattributes.entity.MetricDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MetricDefinitionRepository extends JpaRepository<MetricDefinition, String> {

    // 기존 enum때 equalsIgnoreCase 비겨 동작을 그대로 유지하기 위해 대소문자 무시 조회
    @Query("SELECT m FROM MetricDefinition m WHERE lower(m.metricKey) = lower(:metricKey) " )
    Optional<MetricDefinition> findByMetricKeyIgnoreCase(@Param("metricKey") String metricKey);

    // metricKey 여러 개를 한 번에 배치 조회 (N+1 방지, 대소문자 무시)
    // 주의: IN절은 원소마다 lower()를 적용해주지 않으므로, lowerMetricKeys는 호출 쪽에서
    // 미리 소문자로 변환해서 넘겨야 함 - 안 그러면 대소문자가 다른 키는 조용히 매칭 실패함
    @Query("SELECT m FROM MetricDefinition m WHERE lower(m.metricKey) IN :lowerMetricKeys")
    List<MetricDefinition> findByMetricKeyInIgnoreCase(@Param("lowerMetricKeys") Collection<String> lowerMetricKeys);

}
