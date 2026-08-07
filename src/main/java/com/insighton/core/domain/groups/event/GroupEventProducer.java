package com.insighton.core.domain.groups.event;

import com.insighton.core.common.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
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

        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.CORE_EVENTS_EXCHANGE,
                    RabbitConfig.GROUP_DELETED_ROUTING_KEY,
                    event);

            log.info("[GroupEventProducer] 그룹 삭제 메시지 발행 완료 - Group ID: {}", groupId);

        } catch (AmqpException e) {
            log.error("[GroupEventProducer] 그룹 삭제 메시지 발행 실패 - Group ID: {}, Location IDs: {}, Error: {}",
                    groupId, locationIds, e.getMessage(), e);
            throw e;
        }
    }
}
