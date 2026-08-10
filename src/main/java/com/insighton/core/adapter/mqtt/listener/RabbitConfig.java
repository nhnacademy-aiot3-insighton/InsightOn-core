package com.insighton.core.adapter.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String CORE_EVENTS_EXCHANGE = "insighton.core-events";
    public static final String GROUP_DELETED_QUEUE = "ai-service.group-deleted.queue";
    public static final String LOCATION_DELETED_QUEUE = "ai-service.location-deleted.queue";
    public static final String GROUP_DELETED_ROUTING_KEY = "group.deleted";
    public static final String LOCATION_DELETED_ROUTING_KEY = "location.deleted";

    public static final String TELEMETRY_EXCHANGE = "insighton.core.telemetry.exchange";
    public static final String TELEMETRY_ROUTING_KEY_PREFIX = "insighton.telemetry.";

    @Bean
    public TopicExchange telemetryExchange() {
        return new TopicExchange(TELEMETRY_EXCHANGE, true, false);
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
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
