package com.insighton.core.widgets.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxTable;
import com.insighton.core.domain.widgets.repository.InfluxDbRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class InfluxDbRepositoryTest {

    @Mock
    private InfluxDBClient influxDBClient;

    @InjectMocks
    private InfluxDbRepository influxDbRepository;

    @Test
    @DisplayName("InfluxDB Query API 호출 테스트")
    void query_success() {
        QueryApi mockQueryApi = mock(QueryApi.class);
        FluxTable mockTable = mock(FluxTable.class);
        given(influxDBClient.getQueryApi()).willReturn(mockQueryApi);
        given(mockQueryApi.query("from(bucket: \"test\")")).willReturn(List.of(mockTable));

        List<FluxTable> tables = influxDbRepository.query("from(bucket: \"test\")");

        assertThat(tables).hasSize(1);
    }
}
