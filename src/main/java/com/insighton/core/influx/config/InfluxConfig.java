package com.insighton.core.influx.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteOptions;
import com.influxdb.client.write.events.WriteErrorEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class InfluxConfig {

    @Value("${influx.url}")
    private String url;

    @Value("${influx.token}")
    private String token;

    @Value("${influx.org}")
    private String influxOrg;

    @Value("${influx.bucket}")
    private String bucket;

    /**
     * 주입된 연결 설정을 사용해 InfluxDB 클라이언트를 생성한다.
     *
     * @return InfluxDB 연결 클라이언트
     */
    @Bean(destroyMethod = "close")
    public InfluxDBClient influxDBClient() {
        return InfluxDBClientFactory.create(url, token.toCharArray(), influxOrg, bucket);
    }

    /**
     * InfluxDB 비동기 배치 쓰기를 위한 {@code WriteApi}를 생성한다.
     *
     * <p>배치 쓰기 실패 시 경고를 기록하며 데이터가 드롭될 수 있다.</p>
     *
     * @param influxDBClient InfluxDB 연결 클라이언트
     * @return 비동기 배치 쓰기에 구성된 {@code WriteApi}
     */
    @Bean(destroyMethod = "close")
    public WriteApi writeApi(InfluxDBClient influxDBClient) {

        //TODO: batchSize 조절하기
        // 현재는 기본 값 테스트 후 추후 조정 필요
        WriteOptions options = WriteOptions.builder()
                .batchSize(1000) // 몇 포인트 모이면 플러시 할 지
                .flushInterval(1000) // 몇 ms마다 강제 플러시 할지 (배치 안 찼어도 강제 플러시)
                .bufferLimit(10000) //재시도 대기 큐 최대 크기 (장애 시 이 이상 쌓이면 드롭)
                .build();

        WriteApi writeApi = influxDBClient.makeWriteApi(options);

        writeApi.listenEvents(WriteErrorEvent.class, event ->
                log.warn("InfluxDB 비동기 배치 쓰기 실패, 데이터 드롭", event.getThrowable()));

        return writeApi;
    }
}
