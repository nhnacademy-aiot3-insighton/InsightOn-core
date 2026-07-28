package com.insighton.core.devices.entity;

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
@Table(name = "sensor_devices")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;

    // ================= [센서 전용 필드들] =================
    private Long gatewaysId; // SENSOR 전용 (ACTUATOR는 null)

    @Column(name = "device_eui", unique = true) // nullable=false 제거 (ACTUATOR는 null)
    private String deviceEui;

    private OffsetDateTime lastSeenAt; // SENSOR 전용 (ACTUATOR는 null)

    // ================= [공통 필드들] =================

    @Column(name = "device_name", nullable = false)
    private String deviceName;

    private Long locationsId;

    private OffsetDateTime createdAt;



    public void updateLocation(Long newLocationId){
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