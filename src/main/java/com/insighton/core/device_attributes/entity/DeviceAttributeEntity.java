package com.insighton.core.device_attributes.entity;

import com.insighton.core.sensors.entity.DeviceEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * 장치별 세부 속성(메트릭 key, 최신 상태값 등) 정보를 저장하는 JPA 엔티티 클래스.
 * <p>
 * 동일한 장치 내에서 중복된 메트릭 키가 생성되지 않도록 (device_id, metric_key) 복합 UNIQUE 제약조건이 걸려있습니다.
 */
@Entity
@Table(name = "sensor_device_attributes",
    uniqueConstraints = @UniqueConstraint(
            columnNames = {"device_id", "metric_key"}
    ))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceAttributeEntity {

    /**
     * 장치 속성 기본키 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_attribute_id")
    private Long deviceAttributeId;

    /**
     * 속성을 소유한 부모 장치 엔티티 (FK)
     * <p>
     * 지연 로딩(LAZY)을 사용하여 불필요한 N+1 조회 성능 저하를 방지합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity deviceId;

    /**
     * 수집 패킷 내 JSON Key 또는 제어 명령 필드 식별자 (ex. "co2", "temperature", "power_status")
     */
    @NotNull
    @Column(name = "metric_key", length = 50, nullable = false)
    private String metricKey;

    /**
     * 액추에이터 및 가상 시뮬레이터 전용 최신 수치/텍스트 상태값 저장 필드 (ex. "ON", "OFF", "COOL", "24.0")
     */
    private String currentValueStr;

    @Column(name = "group_id", nullable = false)
    private Long groupId; // 소속 회사/그룹 ID (FK)

    /**
     * 액추에이터 제어 명령 집행 또는 상태 변경 시 최신 상태값을 업데이트하는 비즈니스 메서드
     *
     * @param newValue 새롭게 변경될 상태/수치 문자열
     */
    public void updateCurrentValue(String newValue){
        this.currentValueStr = newValue;
    }
}