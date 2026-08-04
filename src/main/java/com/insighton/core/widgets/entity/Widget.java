package com.insighton.core.widgets.entity;

import com.insighton.core.dashboards.entity.Dashboard;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "widgets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Widget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long widgetId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "dashboard_id",
            nullable = false,
            unique = true
    )
    private Dashboard dashboard;

    @Column(nullable = false)
    private int xPos;
    @Column(nullable = false)
    private int yPos;
    @Column(nullable = false)
    private int width;
    @Column(nullable = false)
    private int height;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "widget_config",
            columnDefinition = "jsonb"
    )
    private WidgetConfig widgetConfig;


    /**
     * {@code @Builder} 패턴을 적용한 생성자
     * - 엔티티 최초 생성 시 필요한 필드(type, metricKey, xPos, yPos, width, height)만 빌더로 넘겨받아 안전하게 객체를 생성합니다.
     * - DB에서 자동 생성되는 PK(widgetId)와 연관관계 매핑 필드(dashboard)는 빌더 호출 후 별도 처리하거나 필요한 경우 추가합니다.
     */
    @Builder
    public Widget(Dashboard dashboard, int xPos, int yPos, int width, int height, WidgetConfig widgetConfig) {
        this.dashboard = dashboard;
        this.xPos = xPos;
        this.yPos = yPos;
        this.width = width;
        this.height = height;
        this.widgetConfig = widgetConfig;
    }

    public void updateㅣocationWidget(int xPos, int yPos, int width, int height) {
        this.xPos = xPos;
        this.yPos = yPos;
        this.width = width;
        this.height = height;
    }

    public void updateWidget(WidgetConfig widgetConfig) {
        this.widgetConfig = widgetConfig;
    }

    public enum Type {
        LINE_CHART_GAUGE, TOGGLE_SWITCH
    }
}
