package com.insighton.core.domain.sensorattributes.entity;

import jakarta.persistence.*;
import lombok.*;



@Entity
@Table(name = "metric_definitions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricDefinition {
    // 기본값 시딩은 MetricDefinitionSeeder(ApplicationRunner)가 앱 시작 시 자동으로 처리함

    @Id
    @Column(name = "metric_key" ,length = 50)
    private String metricKey;

    @Column(name = "metric_name", length = 100, nullable = false)
    private String metricName;

    @Column(name = "unit")
    private String unit;

}