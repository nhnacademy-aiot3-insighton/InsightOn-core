package com.insighton.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class InsightOnCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsightOnCoreApplication.class, args);
    }

}
