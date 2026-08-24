package com.insighton.core.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitConfig {

    public static final String CORE_EVENTS_EXCHANGE = "insighton.core-events";
    public static final String GROUP_DELETED_QUEUE = "ai-service.group-deleted.queue";
    public static final String LOCATION_DELETED_QUEUE = "ai-service.location-deleted.queue";
    public static final String GROUP_DELETED_ROUTING_KEY = "group.deleted";
    public static final String LOCATION_DELETED_ROUTING_KEY = "location.deleted";
    public static final String GATEWAY_STATUS_QUEUE = "ai-service.gateway-status.queue";
    public static final String GATEWAY_STATUS_ROUTING_KEY = "gateway.status";


    // 액추에이터, 센서 삭제 이벤트
    public static final String ACTUATOR_DELETED_QUEUE = "ai-service.actuator-deleted.queue";
    public static final String SENSOR_DELETED_QUEUE = "ai-service.sensor-deleted.queue";
    public static final String ACTUATOR_DELETED_ROUTING_KEY = "actuator.deleted";
    public static final String SENSOR_DELETED_ROUTING_KEY = "sensor.deleted";

    //    public static final String TELEMETRY_EXCHANGE = "insighton.core.telemetry.exchange";
    public static final String TELEMETRY_EXCHANGE = "insighton.core.telemetry.exchange-v2";
    public static final String TELEMETRY_HASH_HEADER = "locationId";

    @Bean
    public CustomExchange telemetryExchange() {

        return new CustomExchange(TELEMETRY_EXCHANGE, "x-consistent-hash", true, false, Map.of("hash-header", TELEMETRY_HASH_HEADER));
    }

    @Bean
    public TopicExchange coreEventsExchange() {
        return new TopicExchange(CORE_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue groupDeletedQueue() {
        return new Queue(GROUP_DELETED_QUEUE, true);
    }

    @Bean
    public Queue locationDeletedQueue() {
        return new Queue(LOCATION_DELETED_QUEUE, true);
    }

    @Bean
    public Queue gatewayStatusQueue() {
        return new Queue(GATEWAY_STATUS_QUEUE, true);
    }

    @Bean
    public Queue actuatorDeletedQueue() {
        return new Queue(ACTUATOR_DELETED_QUEUE, true);
    }

    @Bean
    public Queue sensorDeletedQueue() {
        return new Queue(SENSOR_DELETED_QUEUE, true);
    }

    @Bean
    public Binding groupDeletedBinding(Queue groupDeletedQueue, @Qualifier("coreEventsExchange") TopicExchange coreEventExchange) {
        return BindingBuilder.bind(groupDeletedQueue)
                .to(coreEventExchange)
                .with(GROUP_DELETED_ROUTING_KEY);
    }

    @Bean
    public Binding locationDeletedBinding(Queue locationDeletedQueue, @Qualifier("coreEventsExchange") TopicExchange coreEventExchange) {
        return BindingBuilder.bind(locationDeletedQueue)
                .to(coreEventExchange)
                .with(LOCATION_DELETED_ROUTING_KEY);
    }

    @Bean
    public Binding gatewayStatusBinding(Queue gatewayStatusQueue,
                                        @Qualifier("coreEventsExchange") TopicExchange coreEventExchange) {
        return BindingBuilder.bind(gatewayStatusQueue)
                .to(coreEventExchange)
                .with(GATEWAY_STATUS_ROUTING_KEY);
    }

    @Bean
    public Binding actuatorDeletedBinding(Queue actuatorDeletedQueue, @Qualifier("coreEventsExchange") TopicExchange coreEventExchange) {
        return BindingBuilder.bind(actuatorDeletedQueue)
                .to(coreEventExchange)
                .with(ACTUATOR_DELETED_ROUTING_KEY);
    }

    @Bean
    public Binding sensorDeletedBinding(Queue sensorDeletedQueue, @Qualifier("coreEventsExchange") TopicExchange coreEventExchange) {
        return BindingBuilder.bind(sensorDeletedQueue)
                .to(coreEventExchange)
                .with(SENSOR_DELETED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
