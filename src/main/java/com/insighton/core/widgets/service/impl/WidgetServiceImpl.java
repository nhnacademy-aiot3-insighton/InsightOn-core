package com.insighton.core.widgets.service.impl;

import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.insighton.core.dashboards.entity.Dashboard;
import com.insighton.core.widgets.dto.chart.ChartDataResponse;
import com.insighton.core.widgets.dto.chart.ChartDataset;
import com.insighton.core.widgets.dto.request.WidgetSaveRequest;
import com.insighton.core.widgets.dto.response.WidgetsListResponse;
import com.insighton.core.widgets.entity.Widget;
import com.insighton.core.widgets.entity.WidgetConfig;
import com.insighton.core.widgets.exception.WidgetConfigNotFoundException;
import com.insighton.core.widgets.exception.WidgetNotFoundException;
import com.insighton.core.widgets.repository.InfluxDbRepository;
import com.insighton.core.widgets.repository.WidgetRepository;
import com.insighton.core.widgets.service.WidgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WidgetServiceImpl implements WidgetService {
    private final WidgetRepository widgetRepository;
    private final InfluxDbRepository influxDbRepository;

    private final Map<Long, WidgetConfig> configCache = new ConcurrentHashMap<>();

    private final String BUCKET_NAME = "insighton-bucket";
    private final String MEASUREMENT_NAME = "sensor_data";

    @Override
    @Transactional
    public Long createWidget(Dashboard dashboard, WidgetSaveRequest request) {

        Widget widget = Widget.builder()
                .dashboard(dashboard)
                .xPos(request.xPos() != null ? request.xPos() : 0)
                .yPos(request.yPos() != null ? request.yPos() : 0)
                .width(request.width() != null ? request.width() : 1)
                .height(request.height() != null ? request.height() : 1)
                .widgetConfig(request.widgetConfig())
                .build();
        Widget newWidget = widgetRepository.save(widget);

        return newWidget.getWidgetId();


    }

    @Override
    @Transactional
    public void updateWidget(Long dashboardId, Long targetWidgetId, WidgetSaveRequest request) {
        // 존재하는지 확인
        Widget widget = widgetRepository.findByWidgetIdAndDashboardDashboardsId(targetWidgetId, dashboardId)
                .orElseThrow(() -> WidgetNotFoundException.notFoundWidgetByWidgetId(targetWidgetId));

        if (request.widgetConfig() != null) {
            widget.updateWidget(request.widgetConfig());

            configCache.remove(targetWidgetId);
        }

        if (request.xPos() != null && request.yPos() != null
                && request.width() != null && request.height() != null) {

            widget.updateLocationWidget(
                    request.xPos(),
                    request.yPos(),
                    request.width(),
                    request.height()
            );
        }

    }

    @Override
    @Transactional(readOnly = true)
    public List<WidgetsListResponse> getWidgetList(Long dashboardId) {

        return widgetRepository.findByDashboardDashboardsId(dashboardId)
                .orElseThrow(() -> WidgetNotFoundException.notFoundWidgetByDashboardId(dashboardId));
    }

    @Override
    @Transactional
    public void deleteWidget(Long dashboardId, Long targetWidgetId) {

        widgetRepository.deleteByWidgetIdAndDashboardDashboardsId(targetWidgetId, dashboardId);

        configCache.remove(targetWidgetId);
    }

    @Override
    @Transactional
    public void deleteAllWidget(Long dashboardId) {
        List<Long> widgetIds = widgetRepository.findWidgetIdsByDashboardDashboardsId(dashboardId);

        evictCacheForWidgetIds(widgetIds);

        widgetRepository.deleteAllByDashboardDashboardsId(dashboardId);
    }

    @Override
    @Transactional
    public ChartDataResponse getWidgetChartData(Long targetWidgetId) {

        WidgetConfig config = getWidgetConfigFromCache(targetWidgetId);

        List<FluxTable> tables = getWidgetData(config);

        return convertToChartData(tables);
    }

    @Override
    public void evictCacheForWidgetIds(List<Long> widgetIds) {
        if (widgetIds != null && !widgetIds.isEmpty()) {
            widgetIds.forEach(configCache::remove);
        }
    }

    // ===================== private method ========================

    private WidgetConfig getWidgetConfigFromCache(Long widgetId) {
        return configCache.computeIfAbsent(widgetId, id -> {
            Widget widget = widgetRepository.findById(id)
                    .orElseThrow(() -> WidgetNotFoundException.notFoundWidgetByWidgetId(widgetId));

            WidgetConfig config = widget.getWidgetConfig();

            // id는 존재하는데 config 값이 null 일때는 exception 던지기
            if (config == null) {
                throw new WidgetConfigNotFoundException(widgetId);
            }

            return config;
        });
    }

    /**
     * List<FluxTable>을 DTO로 변환
     */
    private ChartDataResponse convertToChartData(List<FluxTable> tables) {
        Set<String> timeLabels = new TreeSet<>(); // 중복 없이 담기 위해
        Map<String, Map<String, Object>> fieldTimeValueMap = new HashMap<>();


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
                .withZone(ZoneId.systemDefault());

        if (tables != null) {
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    // X축 시간 라벨 모으기 (중복 제거 & 순서 보장)
                    if (record.getTime() != null) {
                        timeLabels.add(formatter.format(record.getTime()));
                    }
                    String timeStr = formatter.format(record.getTime());
                    String fieldName = record.getField();
                    Object value = record.getValue();

                    // y축 값 가져오기 (예시 : co2, temperature(?))
                    fieldTimeValueMap
                            .computeIfAbsent(fieldName, k -> new HashMap<>())
                            .put(timeStr, value);
                }
            }
        }

        // map에 모인 데이터를 chartDataset 객체 리스트로 변환
        List<ChartDataset> datasets = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : fieldTimeValueMap.entrySet()) {
            String fieldName = entry.getKey(); // co2 등의 센서 이름
            Map<String, Object> timeValueMap = entry.getValue(); // 센서의 측정 시간별로 담아둔 센서 값 상자

            List<Object> values = new ArrayList<>();

            // 전체 시간 라벨["10:00", "10:05", "10:10"]을 순서대로 순회
            for (String time : timeLabels) {
                // 해당 시간에 값이 있으면 넣고, 없으면 null을 넣어서 자리를 추기 위해 getOrDefault 사용
                values.add(timeValueMap.getOrDefault(time, null));
            }
            datasets.add(new ChartDataset(fieldName, values));
        }


        return new ChartDataResponse(new ArrayList<>(timeLabels), datasets);
    }

    /**
     * influxDB에서 정보 받아오기
     */
    private List<FluxTable> getWidgetData(WidgetConfig widgetConfig) {

        String fluxQuery = buildFluxQuery(widgetConfig);

        return influxDbRepository.query(fluxQuery);
    }

    /**
     * influxDB query 작성
     */
    private String buildFluxQuery(WidgetConfig config) {
        // 문자열 합치기 작업을 메모리 낭비 없이 빠르게 처리하기 때문에 사용
        StringBuilder flux = new StringBuilder();

        flux.append("from(bucket: \"").append(BUCKET_NAME).append("\")\n")
                .append("   |> range(start: ").append(config.range()).append(")\n")
                .append("   |> filter(fn: (r) => r._measurement == \"").append(MEASUREMENT_NAME).append("\")\n");

        // null이나 빈 값 체크
        if (Objects.nonNull(config.deviceEui()) && !config.deviceEui().isBlank()) {
            flux.append("   |> filter(fn: (r) => r.deviceEui == \"").append(config.deviceEui()).append("\")\n");
        }

        if (Objects.nonNull(config.fields()) && !config.fields().isEmpty()) {
            String fieldFilter = config.fields().stream()
                    .map(f -> "r._field == \"" + f + "\"")
                    .collect(Collectors.joining(" or ")); // list에 담긴 만큼 꺼내서 쿼리문을 작성하도록 코드 구성 (그래서 1개를 보고 싶던 여러개를 보고 싶던 상관이 없음)
            flux.append("   |> filter(fn: (r) => ").append(fieldFilter).append(")\n");
        }

        if (Objects.nonNull(config.type())) {
            switch (config.type()) {
                case GAUGE, SINGLE_STAT:
                    // 가장 최근 데이터 1개만 조회
                    flux.append("   |> last()\n");
                    break;
                case GRAPH:
                default:
                    // 시계열 그래프는 설정한 주기(aggregateWindow)마다 평균(mean) 값으로 묶어서 가져옴
                    if (Objects.nonNull(config.aggregateWindow())) {
                        flux.append("   |> aggregateWindow(every: ").append(config.aggregateWindow())
                                .append(", fn: mean, createEmpty: false)\n");
                    }
                    break;
            }
        }

        flux.append("   |> yield(name: \"result\")");

        return flux.toString();
    }
}
