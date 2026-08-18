package com.insighton.core.domain.groups.event;

import com.insighton.core.domain.region.service.RegionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GroupEventListenerTest {

    @Mock
    private GroupEventProducer groupEventProducer;

    @Mock
    private RegionService regionService;

    @InjectMocks
    private GroupEventListener groupEventListener;

    @Test
    @DisplayName("handleGroupDeleted 호출 시 groupEventProducer.sendGroupDeleteEvent 실행")
    void handleGroupDeleted_success() {
        // given
        GroupDeletedEvent event = new GroupDeletedEvent(1L, List.of(10L, 20L));

        // when
        groupEventListener.handleGroupDeleted(event);

        // then
        verify(groupEventProducer, times(1)).sendGroupDeleteEvent(1L, List.of(10L, 20L));
    }

    @Test
    @DisplayName("recover 호출 시 에러 로그 기록 후 예외 없이 정상 종료")
    void recover_success() {
        // given
        GroupDeletedEvent event = new GroupDeletedEvent(1L, List.of(10L, 20L));
        Exception ex = new RuntimeException("RabbitMQ connection failure");

        // when & then
        assertDoesNotThrow(() -> groupEventListener.recover(ex, event));
    }

    @Test
    @DisplayName("handleGroupRegionUpdated 호출 시 regionService.cacheGroupRegion 실행")
    void handleGroupRegionUpdated_success() {
        // given
        GroupRegionUpdateEvent event = new GroupRegionUpdateEvent(1L, "SEOUL");

        // when
        groupEventListener.handleGroupRegionUpdated(event);

        // then
        verify(regionService, times(1)).cacheGroupRegion(1L, "SEOUL");
    }
}
