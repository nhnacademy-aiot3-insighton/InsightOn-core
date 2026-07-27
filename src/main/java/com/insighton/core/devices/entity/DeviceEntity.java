package com.insighton.core.devices.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

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

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "gateways_id")
    private Long gatewaysId;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "locations_id")
    private Long locationsId;

    @Column(name = "device_eui", nullable = false, unique = true)
    private String deviceEui;

    // 필드명을 deviceName으로 통일
    @Column(name = "device_name", nullable = false)
    @NotNull
    private String deviceName;

    private OffsetDateTime lastSeenAt; // 마지막 통신 시간

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