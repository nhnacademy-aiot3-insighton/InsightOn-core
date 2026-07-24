package com.insighton.core.location.entity;

import com.insighton.core.groups.entity.Groups;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "locations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Locations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long locationId;
    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩 설정으로 성능 최적화
    @JoinColumn(
            name = "group_id",
            nullable = false)
    private Groups groups;
    @Column(nullable = false)
    private String locationName;
    @Column(updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AutoControlMode autoControlMode = AutoControlMode.SUGGESTION;

    /**
     * {@code @Builder} 패턴을 적용한 생성자
     * - 엔티티 최초 가입/생성 시 필요한 필드(groups, locationName, autoControlMode)만 빌더로 넘겨받아 안전하게 객체를 생성합니다.
     * - DB에서 자동 생성되는 PK(locationId)와 @PrePersist로 자동 설정되는 가입 일자(createdAt)는 제외하여 실수를 방지합니다.
     */
    @Builder
    public Locations(Groups groups, String locationName, AutoControlMode autoControlMode) {
        this.groups = groups;
        this.locationName = locationName;
        if (autoControlMode != null) {
            this.autoControlMode = autoControlMode;
        }
    }

    /**
     * JPA 영속화(DB Insert) 직전에 실행되어 자동으로 생성 일시를 주입합니다.
     * 매번 Service 레이어에서 일일이 LocalDateTime.now()를 세팅해줄 필요가 없어 편리합니다.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * 자동 제어 모드를 반전시킵니다.
     * SUGGESTION <-> AI_DIRECT
     */
    public void toggleAutoControlMode() {
        this.autoControlMode = (this.autoControlMode == AutoControlMode.SUGGESTION)
                ? AutoControlMode.AI_DIRECT : AutoControlMode.SUGGESTION;
    }

    public enum AutoControlMode {
        SUGGESTION, AI_DIRECT
    }

}
