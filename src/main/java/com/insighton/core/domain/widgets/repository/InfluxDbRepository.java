package com.insighton.core.domain.widgets.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class InfluxDbRepository {

    private final InfluxDBClient influxDBClient;

    public List<FluxTable> query(String fluxQuery) {
        // QueryApi를 추출하여 Flux 쿼리 실행
        log.info("influxDB 데이터 블러오는 query 실행");
        return influxDBClient.getQueryApi().query(fluxQuery);
    }
}
