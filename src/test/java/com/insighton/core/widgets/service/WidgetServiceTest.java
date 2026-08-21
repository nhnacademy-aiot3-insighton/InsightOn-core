package com.insighton.core.widgets.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.widgets.dto.chart.ChartDataResponse;
import com.insighton.core.domain.widgets.dto.request.WidgetSaveRequest;
import com.insighton.core.domain.widgets.dto.response.WidgetsListResponse;
import com.insighton.core.domain.widgets.entity.Widget;
import com.insighton.core.domain.widgets.entity.WidgetConfig;
import com.insighton.core.domain.widgets.exception.InvalidDateTimeFormatException;
import com.insighton.core.domain.widgets.exception.WidgetConfigNotFoundException;
import com.insighton.core.domain.widgets.exception.WidgetNotFoundException;
import com.insighton.core.domain.widgets.repository.InfluxDbRepository;
import com.insighton.core.domain.widgets.repository.WidgetRepository;
import com.insighton.core.domain.widgets.service.impl.WidgetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WidgetServiceTest {

    @Mock
    private WidgetRepository widgetRepository;

    @Mock
    private InfluxDbRepository influxDbRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WidgetServiceImpl widgetService;

    @BeforeEach
    void setUp() {
        doReturn(valueOperations).when(redisTemplate).opsForValue();
    }

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("위젯 생성 성공")
        void createWidget_success() {
            // given
            Dashboard mockDashboard = mock(Dashboard.class);
            WidgetConfig config = WidgetConfig.builder()
                    .type(Widget.Type.GRAPH)
                    .sensorEui("DEV_01")
                    .range("-1h")
                    .build();

            WidgetSaveRequest request = WidgetSaveRequest.builder()
                    .xPos(0)
                    .yPos(0)
                    .width(6)
                    .height(4)
                    .widgetConfig(config)
                    .build();

            Widget savedWidget = mock(Widget.class);
            given(savedWidget.getWidgetId()).willReturn(10L);
            given(widgetRepository.save(any(Widget.class))).willReturn(savedWidget);

            // when
            Long widgetId = widgetService.createWidget(mockDashboard, request);

            // then
            assertThat(widgetId).isEqualTo(10L);
            verify(widgetRepository, times(1)).save(any(Widget.class));
        }

        @Test
        @DisplayName("위젯 수정 성공 - Config 및 위치 좌표 수정")
        void updateWidget_success() {
            // given
            Long dashboardId = 1L;
            Long widgetId = 10L;

            Widget mockWidget = mock(Widget.class);
            given(widgetRepository.findByWidgetIdAndDashboardDashboardId(widgetId, dashboardId))
                    .willReturn(Optional.of(mockWidget));

            WidgetConfig newConfig = WidgetConfig.builder()
                    .type(Widget.Type.SINGLE_STAT)
                    .sensorEui("DEV_02")
                    .range("-24h")
                    .build();

            WidgetSaveRequest request = WidgetSaveRequest.builder()
                    .widgetId(widgetId)
                    .xPos(2)
                    .yPos(2)
                    .width(4)
                    .height(4)
                    .widgetConfig(newConfig)
                    .build();

            // when
            widgetService.updateWidget(dashboardId, widgetId, request);

            // then
            verify(mockWidget, times(1)).updateWidget(newConfig);
            verify(mockWidget, times(1)).updateLocationWidget(2, 2, 4, 4);
        }

        @Test
        @DisplayName("대시보드에 속한 위젯 리스트 조회 성공")
        void getWidgetList_success() {
            // given
            Long dashboardId = 1L;
            Dashboard mockDashboard = mock(Dashboard.class);
            given(mockDashboard.getDashboardId()).willReturn(dashboardId);

            Widget mockWidget = mock(Widget.class);
            given(mockWidget.getWidgetId()).willReturn(10L);
            given(mockWidget.getDashboard()).willReturn(mockDashboard);
            given(mockWidget.getXPos()).willReturn(0);
            given(mockWidget.getYPos()).willReturn(0);
            given(mockWidget.getWidth()).willReturn(6);
            given(mockWidget.getHeight()).willReturn(4);

            given(widgetRepository.findAllByDashboardDashboardId(dashboardId))
                    .willReturn(List.of(mockWidget));

            // when
            List<WidgetsListResponse> responses = widgetService.getWidgetList(dashboardId);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.getFirst().widgetId()).isEqualTo(10L);
            assertThat(responses.getFirst().dashboardId()).isEqualTo(dashboardId);
        }

        @Test
        @DisplayName("위젯 삭제 성공 - 단일 위젯 삭제 및 캐시 제거")
        void deleteWidget_success() {
            // given
            Long dashboardId = 1L;
            Long widgetId = 10L;

            // when
            widgetService.deleteWidget(dashboardId, widgetId);

            // then
            verify(widgetRepository, times(1)).deleteByWidgetIdAndDashboardDashboardId(widgetId, dashboardId);
        }

        @Test
        @DisplayName("대시보드 하위 전체 위젯 삭제 성공")
        void deleteAllWidget_success() {
            // given
            Long dashboardId = 1L;
            Widget mockWidget = mock(Widget.class);
            given(mockWidget.getWidgetId()).willReturn(10L);
            given(widgetRepository.findAllByDashboardDashboardId(dashboardId)).willReturn(List.of(mockWidget));

            // when
            widgetService.deleteAllWidget(dashboardId);

            // then
            verify(widgetRepository, times(1)).deleteAllByDashboardDashboardId(dashboardId);
        }

        @Test
        @DisplayName("대시보드 ID별 위젯 ID 리스트 조회 성공")
        void getWidgetIdsByDashboardId_success() {
            // given
            Long dashboardId = 1L;
            Widget mockWidget1 = mock(Widget.class);
            given(mockWidget1.getWidgetId()).willReturn(10L);

            Widget mockWidget2 = mock(Widget.class);
            given(mockWidget2.getWidgetId()).willReturn(20L);

            given(widgetRepository.findAllByDashboardDashboardId(dashboardId))
                    .willReturn(List.of(mockWidget1, mockWidget2));

            // when
            List<Long> widgetIds = widgetService.getWidgetIdsByDashboardId(dashboardId);

            // then
            assertThat(widgetIds).containsExactly(10L, 20L);
        }

        @Test
        @DisplayName("위젯 차트 데이터 조회 성공 - Config 기반 InfluxDB 쿼리 실행 및 파싱")
        void getWidgetChartData_success() {
            // given
            Long widgetId = 10L;
            Widget mockWidget = mock(Widget.class);
            WidgetConfig config = WidgetConfig.builder()
                    .type(Widget.Type.GRAPH)
                    .sensorEui("DEV_01")
                    .range("-1h")
                    .aggregateWindow("1m")
                    .fields(List.of("temperature"))
                    .build();

            given(mockWidget.getWidgetConfig()).willReturn(config);
            given(widgetRepository.findById(widgetId)).willReturn(Optional.of(mockWidget));

            FluxTable mockTable = mock(FluxTable.class);
            FluxRecord mockRecord = mock(FluxRecord.class);

            given(mockRecord.getTime()).willReturn(Instant.now());
            given(mockRecord.getField()).willReturn("temperature");
            given(mockRecord.getValue()).willReturn(25.5);
            given(mockTable.getRecords()).willReturn(List.of(mockRecord));

            given(influxDbRepository.query(anyString())).willReturn(List.of(mockTable));

            // when
            ChartDataResponse response = widgetService.getWidgetChartData(widgetId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.labels()).hasSize(1);
            assertThat(response.datasets()).hasSize(1);
            assertThat(response.datasets().getFirst().label()).isEqualTo("temperature");

            verify(influxDbRepository, times(1)).query(anyString());
        }

        @Test
        @DisplayName("SINGLE_STAT 타입 위젯 차트 데이터 조회 성공")
        void getWidgetChartData_singleStat_success() {
            Long widgetId = 10L;
            Widget mockWidget = mock(Widget.class);
            WidgetConfig config = WidgetConfig.builder()
                    .type(Widget.Type.SINGLE_STAT)
                    .sensorEui("DEV_01")
                    .range("-1h")
                    .fields(List.of("temperature"))
                    .build();

            given(mockWidget.getWidgetConfig()).willReturn(config);
            given(widgetRepository.findById(widgetId)).willReturn(Optional.of(mockWidget));
            given(influxDbRepository.query(anyString())).willReturn(List.of());

            ChartDataResponse response = widgetService.getWidgetChartData(widgetId);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Redis 캐시된 데이터가 Map 형태일 때 ObjectMapper로 변환하여 조회 성공")
        void getWidgetChartData_cacheHitMap_success() {
            Long widgetId = 10L;
            java.util.Map<String, Object> mapCache = java.util.Map.of("sensorEui", "DEV_01");
            WidgetConfig convertedConfig = WidgetConfig.builder()
                    .type(Widget.Type.GRAPH)
                    .sensorEui("DEV_01")
                    .range("-1h")
                    .build();

            doReturn(mapCache).when(valueOperations).get("widget:config:" + widgetId);
            given(objectMapper.convertValue(mapCache, WidgetConfig.class)).willReturn(convertedConfig);
            given(influxDbRepository.query(anyString())).willReturn(List.of());

            ChartDataResponse response = widgetService.getWidgetChartData(widgetId);
            assertThat(response).isNotNull();
        }
    }

    @Nested
    @DisplayName("실패 및 예외 케이스")
    class FailureCases {

        @Test
        @DisplayName("대시보드 하위에 위젯이 없을 때 빈 리스트 반환")
        void getWidgetList_emptyWidgets_returnsEmptyList() {
            // given
            Long dashboardId = 1L;
            given(widgetRepository.findAllByDashboardDashboardId(dashboardId)).willReturn(List.of());

            // when
            List<WidgetsListResponse> responses = widgetService.getWidgetList(dashboardId);

            // then
            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("위젯 수정 실패 - 존재하지 않는 위젯인 경우 WidgetNotFoundException 발생")
        void updateWidget_notFound() {
            // given
            Long dashboardId = 1L;
            Long widgetId = 999L;
            WidgetSaveRequest request = mock(WidgetSaveRequest.class);

            given(widgetRepository.findByWidgetIdAndDashboardDashboardId(widgetId, dashboardId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> widgetService.updateWidget(dashboardId, widgetId, request))
                    .isInstanceOf(WidgetNotFoundException.class);
        }

        @Test
        @DisplayName("위젯 차트 데이터 조회 실패 - 위젯 Config가 null일 때 WidgetConfigNotFoundException 발생")
        void getWidgetChartData_nullConfig_throwsException() {
            // given
            Long widgetId = 10L;
            Widget mockWidget = mock(Widget.class);
            given(mockWidget.getWidgetConfig()).willReturn(null);
            given(widgetRepository.findById(widgetId)).willReturn(Optional.of(mockWidget));

            // when & then
            assertThatThrownBy(() -> widgetService.getWidgetChartData(widgetId))
                    .isInstanceOf(WidgetConfigNotFoundException.class);
        }

        @Test
        @DisplayName("위젯 차트 데이터 조회 실패 - 잘못된 range 파라미터일 때 InvalidDateTimeFormatException 발생")
        void getWidgetChartData_invalidRange_throwsException() {
            // given
            Long widgetId = 10L;
            Widget mockWidget = mock(Widget.class);
            WidgetConfig invalidConfig = WidgetConfig.builder()
                    .type(Widget.Type.GRAPH)
                    .range("invalid-duration-format") // 잘못된 포맷
                    .build();

            given(mockWidget.getWidgetConfig()).willReturn(invalidConfig);
            given(widgetRepository.findById(widgetId)).willReturn(Optional.of(mockWidget));

            // when & then
            assertThatThrownBy(() -> widgetService.getWidgetChartData(widgetId))
                    .isInstanceOf(InvalidDateTimeFormatException.class);
        }

        @Test
        @DisplayName("위젯 차트 데이터 조회 실패 - 잘못된 aggregateWindow 파라미터일 때 InvalidDateTimeFormatException 발생")
        void getWidgetChartData_invalidAggregateWindow_throwsException() {
            Long widgetId = 10L;
            Widget mockWidget = mock(Widget.class);
            WidgetConfig invalidConfig = WidgetConfig.builder()
                    .type(Widget.Type.GRAPH)
                    .range("-1h")
                    .aggregateWindow("invalid-window")
                    .build();

            given(mockWidget.getWidgetConfig()).willReturn(invalidConfig);
            given(widgetRepository.findById(widgetId)).willReturn(Optional.of(mockWidget));

            assertThatThrownBy(() -> widgetService.getWidgetChartData(widgetId))
                    .isInstanceOf(InvalidDateTimeFormatException.class);
        }
    }
}
