package com.insighton.core.actuators.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity // JPA 엔티티 클래스임을 선언
@Table(name = "actuators") // 매핑할 데이터베이스 테이블명을 actuators로 지정
@Getter // 모든 필드의 Getter 메서드를 자동으로 생성
@NoArgsConstructor // 기본 생성자(Default Constructor)를 생성
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자를 생성
@Builder // 빌더 패턴 객체 생성을 지원
public class ActuatorsEntity {

    @Id // PK(기본키) 필드로 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB의 auto-increment(BIGSERIAL) 전략에 맞게 ID 자동 증가
    @Column(name = "actuator_id") // DB의 actuator_id 컬럼과 매핑
    private Long actuatorId;

    @Column(name = "location_id", nullable = false) // 설치 구역 식별자(FK) 매핑하며 필수값으로 지정
    private Long locationId;

    @Column(name = "device_name", length = 100, nullable = false) // 장비 이름을 100자 이하의 필수값으로 매핑
    private String deviceName;

    @Enumerated(EnumType.STRING) // Enum의 이름 문자열(AIRCON 등) 그대로 DB에 저장되도록 설정
    @Column(name = "actuator_type", length = 30, nullable = false) // 액추에이터 종류를 구분하는 필수 Enum 컬럼 매핑
    private ActuatorType actuatorType;

    @JdbcTypeCode(SqlTypes.JSON) // 하이버네이트의 JSON 타입 변환기를 사용하도록 지정
    @Column(name = "current_state", columnDefinition = "jsonb", nullable = false) // PostgreSQL의 jsonb 컬럼과 매핑되는 상태값 데이터
    private Map<String, Object> currentState;

    @Column(name = "state_updated_at") // 상태값이 최근 변경된 시각을 매핑하는 컬럼
    private OffsetDateTime stateUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false) // 레코드 최초 생성시각을 매핑하며 수정이 불가능하도록 설정
    private OffsetDateTime createdAt;

    // 액추에이터의 최신 상태(JSONB)를 변경하고 수정시각을 현재로 갱신하는 비즈니스 메서드
    public void updateState(Map<String, Object> newState) {
        this.currentState = newState;
        this.stateUpdatedAt = OffsetDateTime.now();
    }

    // 장비 이름이 유효한 경우에만 새 이름으로 수정하는 비즈니스 메서드
    public void updateName(String newDeviceName) {
        if (newDeviceName != null && !newDeviceName.trim().isEmpty()) {
            this.deviceName = newDeviceName;
        }
    }
}