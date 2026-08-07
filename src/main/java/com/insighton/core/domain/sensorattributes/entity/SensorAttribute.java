package com.insighton.core.domain.sensorattributes.entity;

import com.insighton.core.domain.sensors.entity.Sensor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장치별 세부 속성(메트릭 key, 최신 상태값 등) 정보를 저장하는 JPA 엔티티 클래스.
 * <p>
 * 동일한 장치 내에서 중복된 메트릭 키가 생성되지 않도록 (sensor_id, metric_key) 복합 UNIQUE 제약조건이 걸려있습니다.
 */
@Entity
@Table(name = "sensor_attributes",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"sensor_id", "metric_key"}
        ))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorAttribute {

    /**
     * 장치 속성 기본키 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sensor_attribute_id")
    private Long sensorAttributeId;

    /**
     * 속성을 소유한 부모 장치 엔티티 (FK)
     * <p>
     * 지연 로딩(LAZY)을 사용하여 불필요한 N+1 조회 성능 저하를 방지합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    /**
     * 수집 패킷 내 JSON Key 또는 제어 명령 필드 식별자 (ex. "co2", "temperature", "power_status")
     */
    @Column(name = "metric_key", length = 50, nullable = false)
    private String metricKey;

}