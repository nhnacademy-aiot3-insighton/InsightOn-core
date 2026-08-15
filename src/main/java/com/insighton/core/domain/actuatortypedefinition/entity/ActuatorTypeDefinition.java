package com.insighton.core.domain.actuatortypedefinition.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 액추에이터 종류 마스터 데이터 - 그룹에 속하지 않는 전역 카탈로그 (MetricDefinition과 동일한 성격)
@Entity
@Table(name ="actuator_type")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActuatorTypeDefinition {

    @Id
    @Column(name = "type_code", length = 30)
    private String typeCode; // AIRCON

    @Column(name = "type_name", length = 50, nullable = false)
    private String typeName; // 에어컨

    @Column(name = "description", length = 200)
    private String description; // 종류 설명 (선택)

}
