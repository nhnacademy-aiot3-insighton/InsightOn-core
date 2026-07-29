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

    @Bean
    public TopicExchange telemetryExchange() {
        return new TopicExchange(TELEMETRY_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
