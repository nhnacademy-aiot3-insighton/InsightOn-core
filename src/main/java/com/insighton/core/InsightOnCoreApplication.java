package com.insighton.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class InsightOnCoreApplication {

    /**
     * Spring Boot 애플리케이션을 시작합니다.
     *
     * @param args 애플리케이션 실행 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(InsightOnCoreApplication.class, args);
    }

}
