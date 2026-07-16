package com.insighton.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Entity
@Table(name = "device_attributes")
@Getter
@NoArgsConstructor
public class DeviceAttributeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deviceAttributeId;

    @ManyToOne(fetch = FetchType.LAZY) // N+1문제 해결?
    @JoinColumn(name = "device_id")
    private DeviceEntity deviceId;

    @NotNull
    private String metricKey;
    @NotNull
    private String displayName;

    private String unit;

    private String currentValueSte;


}
