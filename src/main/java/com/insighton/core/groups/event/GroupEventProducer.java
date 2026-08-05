package com.insighton.core.groups.event;

import com.insighton.core.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupEventProducer {

    private final RabbitTemplate rabbitTemplate;


    public void sendGroupDeleteEvent(Long groupId, List<Long> locationIds) {
        GroupDeletedEvent event = new GroupDeletedEvent(groupId, locationIds);

        log.info("그룹 삭제 메세지 발행 시작 - Group ID : {}", groupId);

        rabbitTemplate.convertAndSend(
                RabbitConfig.CORE_EVENTS_EXCHANGE,
                RabbitConfig.GROUP_DELETED_ROUTING_KEY,
                event
        );
    }
}
