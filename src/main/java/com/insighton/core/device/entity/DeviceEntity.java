package com.insighton.core.device.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;

/**
 * IoT 장치(Device) 마스터 정보를 관리하는 JPA 엔티티 클래스.
 */
@Entity
@Table(name = "devices")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deviceId;

    private Long gatewaysId;

    private Long locationsId;

    @Column(name = "device_eui", nullable = false, unique = true)
    private String deviceEui;

    // 💡 필드명을 deviceName으로 통일
    @Column(name = "device_name", nullable = false)
    @NotNull
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DeviceType type;

    private ZonedDateTime lastSeenAt;

    private ZonedDateTime createdAt;

    public void updateLocation(Long newLocationId){
        this.locationsId = newLocationId;
    }

    public void updateLastSeen(){
        this.lastSeenAt = ZonedDateTime.now();
    }
}