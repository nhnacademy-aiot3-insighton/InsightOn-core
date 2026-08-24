package com.insighton.core.widgets.entity;

import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.widgets.entity.Widget;
import com.insighton.core.domain.widgets.entity.WidgetConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class WidgetTest {

    @Test
    @DisplayName("위젯 생성 성공 및 updateWidget/updateLocationWidget 테스트")
    void createAndUpdateWidget_success() {
        Dashboard mockDashboard = mock(Dashboard.class);
        WidgetConfig initialConfig = WidgetConfig.builder().type(Widget.Type.GRAPH).build();
        WidgetConfig updatedConfig = WidgetConfig.builder().type(Widget.Type.SINGLE_STAT).build();

        Widget widget = Widget.builder()
                .dashboard(mockDashboard)
                .xPos(0)
                .yPos(0)
                .width(2)
                .height(2)
                .widgetConfig(initialConfig)
                .build();

        assertThat(widget.getXPos()).isEqualTo(0);
        assertThat(widget.getYPos()).isEqualTo(0);
        assertThat(widget.getWidth()).isEqualTo(2);
        assertThat(widget.getHeight()).isEqualTo(2);

        widget.updateWidget(updatedConfig);
        assertThat(widget.getWidgetConfig()).isEqualTo(updatedConfig);

        widget.updateLocationWidget(5, 5, 4, 4);
        assertThat(widget.getXPos()).isEqualTo(5);
        assertThat(widget.getYPos()).isEqualTo(5);
        assertThat(widget.getWidth()).isEqualTo(4);
        assertThat(widget.getHeight()).isEqualTo(4);
    }

    @Test
    @DisplayName("xPos가 음수일 때 IllegalArgumentException 발생")
    void validatePosition_invalidXPos_throwsException() {
        Dashboard mockDashboard = mock(Dashboard.class);
        assertThatThrownBy(() -> Widget.builder()
                .dashboard(mockDashboard)
                .xPos(-1)
                .yPos(0)
                .width(2)
                .height(2)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("yPos가 음수일 때 IllegalArgumentException 발생")
    void validatePosition_invalidYPos_throwsException() {
        Dashboard mockDashboard = mock(Dashboard.class);
        assertThatThrownBy(() -> Widget.builder()
                .dashboard(mockDashboard)
                .xPos(0)
                .yPos(-1)
                .width(2)
                .height(2)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("width가 1 미만일 때 IllegalArgumentException 발생")
    void validateSize_invalidWidth_throwsException() {
        Dashboard mockDashboard = mock(Dashboard.class);
        assertThatThrownBy(() -> Widget.builder()
                .dashboard(mockDashboard)
                .xPos(0)
                .yPos(0)
                .width(0)
                .height(2)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("height가 1 미만일 때 IllegalArgumentException 발생")
    void validateSize_invalidHeight_throwsException() {
        Dashboard mockDashboard = mock(Dashboard.class);
        assertThatThrownBy(() -> Widget.builder()
                .dashboard(mockDashboard)
                .xPos(0)
                .yPos(0)
                .width(2)
                .height(0)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
