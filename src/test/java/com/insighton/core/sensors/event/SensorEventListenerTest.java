package com.insighton.core.sensors.event;

import com.insighton.core.domain.sensors.event.SensorDeletedEvent;
import com.insighton.core.domain.sensors.event.SensorEventListener;
import com.insighton.core.domain.sensors.event.SensorEventProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SensorEventListenerTest {

    @Mock private SensorEventProducer sensorEventProducer;

    @InjectMocks
    private SensorEventListener sensorEventListener;

    @Test
    @DisplayName("handleSensorDeleted - 프로듀서에게 삭제 이벤트를 그대로 전달")
    void 삭제이벤트_처리_성공() {
        SensorDeletedEvent event = new SensorDeletedEvent(1L);

        sensorEventListener.handleSensorDeleted(event);

        verify(sensorEventProducer).sendSensorDeleteEvent(1L);
    }

    @Test
    @DisplayName("handleSensorDeleted - 프로듀서가 예외를 던지면 그대로 전파 (재시도 트리거 조건)")
    void 삭제이벤트_프로듀서예외_전파() {
        SensorDeletedEvent event = new SensorDeletedEvent(1L);
        doThrow(new RuntimeException("발행 실패")).when(sensorEventProducer).sendSensorDeleteEvent(1L);

        assertThrows(RuntimeException.class, () -> sensorEventListener.handleSensorDeleted(event));
    }

    @Test
    @DisplayName("recover - 재시도가 다 실패해도 예외를 던지지 않고 로그만 남김")
    void 재시도소진후_복구_예외없음() {
        SensorDeletedEvent event = new SensorDeletedEvent(1L);

        assertDoesNotThrow(() -> sensorEventListener.recover(new RuntimeException("발행 실패"), event));
    }
}
