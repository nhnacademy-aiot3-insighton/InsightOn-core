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

    @Id
    @Column(name = "metric_key" ,length = 50)
    private String metricKey;

    @Column(name = "metric_name", length = 100, nullable = false)
    private String metricName;

    @Column(name = "unit")
    private String unit;

}