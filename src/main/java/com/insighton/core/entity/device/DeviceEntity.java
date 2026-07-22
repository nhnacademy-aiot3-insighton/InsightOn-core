package com.insighton.core.entity.device;


import com.insighton.core.entity.deviceAttribute.DeviceAttributeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "device")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deviceId;

//    @ManyToOne
//    private gatewaysId gatewaysId;
    private Long gatewaysId;

//    @ManyToOne
//    private locationsId locationsId;
    private Long locationsId;

    @Column(name = "device_eui", nullable = false, unique = true)
    private String deviceEui;
    @NotNull
    private String name;
    @NotNull
    private String type;

    private ZonedDateTime lastSeenAt;
    private ZonedDateTime createdAt;

    public void updateLocation(Long newLocationId){
        this.locationsId = newLocationId;
    }

    public void updateLastSeen(){
        this.lastSeenAt = ZonedDateTime.now();
    }

    @OneToMany(mappedBy = "deviceId")// Attribute쪽 Id랑 매핑
    private List<DeviceAttributeEntity> attributeList = new ArrayList<>();

}
