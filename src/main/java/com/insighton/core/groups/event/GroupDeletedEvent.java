package com.insighton.core.groups.event;

/**
 * 그룹 삭제 완료 시 발행되는 이벤트 객체
 *
 * @param groupId 삭제된 그룹 ID
 */
public record GroupDeletedEvent(
        Long groupId
) {
}
