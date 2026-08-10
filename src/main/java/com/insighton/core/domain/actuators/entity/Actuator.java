package com.insighton.core.domain.actuators.entity;

import com.insighton.core.domain.location.entity.Location;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "actuators")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Actuator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "actuator_id")
    private Long actuatorId; // 액추에이터 PK

    @JoinColumn(name = "location_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Location location; // 장소 ID

    @Column(name = "sensor_name", length = 100, nullable = false)
    private String sensorName; // 센서 이름

    @Enumerated(EnumType.STRING)
    @Column(name = "actuator_type", length = 30, nullable = false)
    private ActuatorType actuatorType; // 액추에이터 종류

    @JdbcTypeCode(SqlTypes.JSON) // Gateway.connectionConfig와 동일한 방식으로 통일
    @Column(name = "current_state", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> currentState; // 현재 제어 상태

    @Column(name = "state_updated_at")
    private OffsetDateTime stateUpdatedAt; // 상태 최종 변경 시각

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // 최초 등록 시간

    // 상태 갱신 -> 변경시간 기록
    public void updateState(Map<String, Object> newState) {
        this.currentState = newState;
        this.stateUpdatedAt = OffsetDateTime.now();
    }

    // 유효한 값일 때만 이름 변경
    public void updateName(String newSensorName) {
        if (newSensorName != null && !newSensorName.trim().isEmpty()) {
            this.sensorName = newSensorName;
        }
    }
}