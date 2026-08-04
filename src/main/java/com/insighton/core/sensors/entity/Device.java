package com.insighton.core.sensors.entity;

import com.insighton.core.gateway.entity.Gateway;
import com.insighton.core.groups.entity.Groups;
import com.insighton.core.location.entity.Locations;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * IoT 장치(Device) 마스터 정보를 관리하는 JPA 엔티티 클래스.
 */
@Entity
@Table(name = "sensors")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deviceId; // 디바이스 PK

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType; // 센서/액추에이터 구분

    // ================= [센서 전용 필드들] =================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name =  "gateway_id", nullable = false)
    private Gateway gatewaysId; // SENSOR 전용 (ACTUATOR는 null)

    @Column(name = "device_eui", length = 50, unique = true, nullable = false)
    private String deviceEui; // 하드웨어 고유 시리얼

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt; // 마지막 통신 시간

    // ================= [공통 필드들] =================

    @Column(name = "device_name", length = 100, nullable = false)
    private String deviceName; // 디바이스 이름

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Locations locationsId; // 장소 아이디

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // 생성 일시

    // =================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name =  "group_id", nullable = false)
    private Groups groupId; // 그룹 아이디

    public void updateLocation(Locations newLocationId){
        this.locationsId = newLocationId;
    }

    public void updateLastSeen(){
        this.lastSeenAt = OffsetDateTime.now();
    }

    public void updateName(String newDeviceName){
        if(newDeviceName != null && !newDeviceName.trim().isEmpty()){
            this.deviceName = newDeviceName;
        }
    }
}