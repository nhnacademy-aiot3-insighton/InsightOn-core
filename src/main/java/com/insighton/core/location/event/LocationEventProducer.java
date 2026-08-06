package com.insighton.core.location.event;

import com.insighton.core.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocationEventProducer {

    private final RabbitTemplate rabbitTemplate;


    public void sendLocationDeleteEvent(Long locationId) {
        try {
            LocationDeletedEvent event = new LocationDeletedEvent(locationId);
            log.info("location 삭제 메세지 발행 시작 - Location ID : {}", locationId);

            rabbitTemplate.convertAndSend(
                    RabbitConfig.CORE_EVENTS_EXCHANGE,
                    RabbitConfig.LOCATION_DELETED_ROUTING_KEY,
                    event
            );

            log.info("[LocationEventProducer] Location 삭제 이벤트 전송 성공 - locationId: {}", locationId);
        } catch (AmqpException e) {
            log.error("[LocationEventProducer] Location 삭제 이벤트 전송 실패 - locationId: {}, error: {}",
                    locationId, e.getMessage(), e);
        }
    }
}
