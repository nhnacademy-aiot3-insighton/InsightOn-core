package com.insighton.core.entity.deviceAttribute;

import com.insighton.core.entity.device.DeviceEntity;
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
@Table(name = "device_attributes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_device_metric",
                        columnNames = {"device_id","metric_key"}
                )
        })
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
    private Long deviceAttributeId;

    /**
     * 속성을 소유한 부모 장치 엔티티 (FK)
     * <p>
     * 지연 로딩(LAZY)을 사용하여 불필요한 N+1 조회 성능 저하를 방지합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private DeviceEntity deviceId;

    /**
     * 수집 패킷 내 JSON Key 또는 제어 명령 필드 식별자 (ex. "co2", "temperature", "power_status")
     */
    @NotNull
    private String metricKey;

    /*
     * 참고: displayName, unit은 DB 테이블 컬럼 대신 MetricDefinition Enum에서 관리되므로 엔티티 필드에서 제외함
     */

    /**
     * 액추에이터 및 가상 시뮬레이터 전용 최신 수치/텍스트 상태값 저장 필드 (ex. "ON", "OFF", "COOL", "24.0")
     */
    private String currentValueStr;

    /**
     * 액추에이터 제어 명령 집행 또는 상태 변경 시 최신 상태값을 업데이트하는 비즈니스 메서드
     *
     * @param newValue 새롭게 변경될 상태/수치 문자열
     */
    public void updateCurrentValue(String newValue){
        this.currentValueStr = newValue;
    }
}