package com.insighton.core.location.event;

import com.insighton.core.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocationEventProducer {

    private final RabbitTemplate rabbitTemplate;


    public void sendLocationDeleteEvent(Long locationId) {
        LocationDeletedEvent event = new LocationDeletedEvent(locationId);

        log.info("location 삭제 메세지 발행 시작 - Location ID : {}", locationId);

        rabbitTemplate.convertAndSend(
                RabbitConfig.CORE_EVENTS_EXCHANGE,
                RabbitConfig.LOCATION_DELETED_ROUTING_KEY,
                event
        );
    }
}
