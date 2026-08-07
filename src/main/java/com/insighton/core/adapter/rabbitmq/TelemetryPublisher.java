package com.insighton.core.adapter.rabbitmq;

import com.insighton.core.common.config.RabbitConfig;
import com.insighton.core.adapter.rabbitmq.dto.TelemetryEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 정제된 Telemetry DTO Rule Engine으로 전파용
 * RabbitMQ 장애가 MQTT 리스너 스레드 막지 않도록 비동기 처리 + Fail-Silent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 정제된 텔레메트리 이벤트를 Rule Engine 전파용 RabbitMQ Topic Exchange에 비동기로 발행함.
     * 라우팅 키는 {@code telemetry.{groupsId}} 형태로, 테넌트(그룹) 단위까지만 라우팅을 나누고
     * location/sensor 단위 필터링은 Rule Engine이 소비 후 인메모리에서 처리함(라우팅을 잘게
     * 쪼개면 유저가 대시보드에서 위치/기기를 추가·삭제할 때마다 큐 바인딩을 갱신해야 해서 피함).
     * {@code @Async}로 비동기 처리하며, 발행 실패는 예외를 던지지 않고 로그만 남기고 드롭함
     * (Fail-Silent) — RabbitMQ 장애가 MQTT 리스너 스레드를 막지 않게 하기 위함.
     *
     * @param event 발행할 텔레메트리 이벤트
     */
    @Async("telemetryDispatchExecutor")
    public void publish(TelemetryEventMessage event) {
        String routingKey = RabbitConfig.TELEMETRY_ROUTING_KEY_PREFIX + event.groupId();

        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.TELEMETRY_EXCHANGE,
                    routingKey,
                    event
            );
        } catch (AmqpException e) {
            log.warn("RabbitMQ 발행 실패, 메시지 드롭 (groupId = {} , locationId = {})", event.groupId(), event.locationId(), e);
        }
    }
}
