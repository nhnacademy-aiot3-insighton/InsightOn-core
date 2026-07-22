package com.insighton.core.rabbitmq.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String TELEMETRY_EXCHANGE = "insighton.core.telemetry.exchange";
    public static final String TELEMETRY_ROUTING_KEY_PREFIX = "insighton.telemetry.";

    /**
     * 텔레메트리 메시지에 사용할 토픽 교환기를 생성합니다.
     *
     * @return 내구성이 있고 자동 삭제되지 않는 텔레메트리 토픽 교환기
     */
    @Bean
    public TopicExchange telemetryExchange() {
        return new TopicExchange(TELEMETRY_EXCHANGE, true, false);
    }

    /**
     * RabbitMQ 메시지를 JSON 형식으로 변환하는 메시지 변환기를 생성합니다.
     *
     * @return JSON 메시지 변환기
     */
    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
