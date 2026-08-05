package com.insighton.core.groups.event;

import java.util.List;

/**
 * 그룹 삭제 완료 시 발행되는 이벤트 객체
 *
 * @param groupId     삭제된 그룹 ID
 * @param locationIds 따라서 삭제될 location ID
 */
public record GroupDeletedEvent(
        Long groupId,
        List<Long> locationIds
) {
}
