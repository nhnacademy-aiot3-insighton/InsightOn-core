package com.insighton.core.domain.sensors.entity;

import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.location.entity.Location;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * IoT 장치(Sensor) 마스터 정보를 관리하는 JPA 엔티티 클래스.
 */
@Entity
@Table(name = "sensors")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sensorId; // 센서 PK

    // ================= [센서 전용 필드들] =================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gateway_id", nullable = false)
    private Gateway gateway;

    @Column(name = "sensor_eui", length = 50, unique = true, nullable = false)
    private String sensorEui; // 하드웨어 고유 시리얼

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt; // 마지막 통신 시간

    // ================= [공통 필드들] =================

    @Column(name = "sensor_name", length = 100, nullable = false)
    private String sensorName; // 센서 이름

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location; // 장소 아이디

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // 생성 일시

    // =================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name =  "group_id", nullable = false)
    private Group group; // 그룹 아이디

    public void updateLocation(Location newLocationId) {
        this.location = newLocationId;
    }

    public void updateLastSeen() {
        this.lastSeenAt = OffsetDateTime.now();
    }

    public void updateName(String newSensorName) {
        if (newSensorName != null && !newSensorName.trim().isEmpty()) {
            this.sensorName = newSensorName;
        }
    }
}