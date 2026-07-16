package com.insighton.core.entity;

import jakarta.persistence.*;
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

    @NotNull
    private String deviceEui;
    @NotNull
    private String name;
    @NotNull
    private String type;

    private ZonedDateTime createdAt;

    @OneToMany(mappedBy = "deviceId")// Attribute쪽 Id랑 매핑
    private List<DeviceAttributeEntity> attributeList = new ArrayList<>();
}
