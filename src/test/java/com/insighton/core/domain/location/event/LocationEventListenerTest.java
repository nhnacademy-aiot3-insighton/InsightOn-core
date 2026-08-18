package com.insighton.core.domain.location.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocationEventListenerTest {

    @Mock
    private LocationEventProducer locationEventProducer;

    @InjectMocks
    private LocationEventListener locationEventListener;

    @Test
    @DisplayName("handleLocationDeleted 호출 시 locationEventProducer.sendLocationDeleteEvent 실행")
    void handleLocationDeleted_success() {
        // given
        LocationDeletedEvent event = new LocationDeletedEvent(1L);

        // when
        locationEventListener.handleLocationDeleted(event);

        // then
        verify(locationEventProducer, times(1)).sendLocationDeleteEvent(1L);
    }

    @Test
    @DisplayName("recover 호출 시 에러 로그 출력 및 예외 없이 종료")
    void recover_success() {
        // given
        LocationDeletedEvent event = new LocationDeletedEvent(1L);
        Exception ex = new RuntimeException("RabbitMQ Connection Error");

        // when & then
        assertDoesNotThrow(() -> locationEventListener.recover(ex, event));
    }
}
