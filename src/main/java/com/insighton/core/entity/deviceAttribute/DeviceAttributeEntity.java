package com.insighton.core.entity.deviceAttribute;

import com.insighton.core.entity.device.DeviceEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deviceAttributeId;

    @ManyToOne(fetch = FetchType.LAZY) // N+1문제 해결?
    @JoinColumn(name = "device_id")
    private DeviceEntity deviceId;

    @NotNull
    private String metricKey;

    // enum에 디스플레이와 유닛이 포함되어있음
//    @NotNull
//    private String displayName;
//
//    private String unit;

    private String currentValueStr;

    // 엑추에이터 상태 변경 시 값을 업데이트
    public void updateCurrentValue(String newValue){
        this.currentValueStr = newValue;
    }
}
