package com.insighton.core.dashboards.entity;

import com.insighton.core.location.entity.Location;
import com.insighton.core.location.exception.EmptyValueException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "dashboards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dashboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dashboardId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "location_id",
            nullable = false,
            unique = true
    )
    private Location location;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    /**
     * {@code @Builder} 패턴을 적용한 생성자
     * - 엔티티 최초 생성 시 필요한 필드(location, title)만 빌더로 넘겨받아 안전하게 객체를 생성합니다.
     * - DB에서 자동 생성되는 PK(dashboardsId)와 대시보드 위젯 격자 구조 변경 시 세팅되는 수정 일시(updatedAt)는 제외하여 실수를 방지합니다.
     */
    @Builder
    public Dashboard(Location location, String title) {
        this.location = location;
        this.title = title;
    }

    public void updateTitle(String newTitle) {
        if (newTitle == null || newTitle.isBlank()) {
            throw new EmptyValueException();
        }
        this.title = newTitle;
    }

    /**
     * 대시보드 내부 위젯 격자 구조 변경 시 최종 수정 일시를 갱신합니다.
     */
    public void updateWidgetLayout() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
