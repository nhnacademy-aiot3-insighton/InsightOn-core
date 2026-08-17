package com.insighton.core.domain.sensors.event;

import com.insighton.core.common.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensorEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendSensorDeleteEvent(Long sensorId) {
        try {
            SensorDeletedEvent event = new SensorDeletedEvent(sensorId);
            log.info("센서 삭제 메세지 발행 시작 - Sensor ID : {}", sensorId);

            rabbitTemplate.convertAndSend(
                    RabbitConfig.CORE_EVENTS_EXCHANGE,
                    RabbitConfig.SENSOR_DELETED_ROUTING_KEY,
                    event
            );

            log.info("[SensorEventProducer] 센서 삭제 이벤트 전송 성공 - sensorId: {}", sensorId);
        } catch (AmqpException e) {
            log.error("[SensorEventProducer] 센서 삭제 이벤트 전송 실패 - sensorId: {}, error: {}",
                    sensorId, e.getMessage(), e);
            throw e;
        }
    }
}
