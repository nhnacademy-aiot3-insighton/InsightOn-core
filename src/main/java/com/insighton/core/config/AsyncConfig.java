package com.insighton.core.config;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    /**
     * 텔레메트리 디스패치 작업을 처리하는 스레드 풀 실행기를 구성한다.
     *
     * @return 초기화된 텔레메트리 디스패치용 스레드 풀 실행기
     */
    @Bean("telemetryDispatchExecutor")
    public ThreadPoolTaskExecutor telemetryDispatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("telemetry-dispatch-");
        executor.setRejectedExecutionHandler(new AbortPolicy());
        executor.initialize();
        return executor;
    }
}

