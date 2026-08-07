package com.insighton.core.sensor_attributes.entity;

import jakarta.persistence.*;
import lombok.*;



@Entity
@Table(name = "metric_definitions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricDefinition {
    // TODO: 메트릭정의 삽입
    //    INSERT INTO metric_definitions (metric_key, metric_name, unit) VALUES
    //            ('co2', '이산화탄소', 'ppm'),
    //            ('temperature', '온도', '°C'),
    //            ('humidity', '습도', '%');
    //    ON CONFLICT (metric_key) DO NOTHING;

    @Id
    @Column(name = "metric_key" ,length = 50)
    private String metricKey;

    @Column(name = "metric_name", length = 100, nullable = false)
    private String metricName;

    @Column(name = "unit")
    private String unit;

}