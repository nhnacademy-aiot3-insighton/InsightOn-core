package com.insighton.core.widgets.controller;

import com.insighton.core.groups.service.CoreManagementUseCase;
import com.insighton.core.widgets.dto.response.WidgetsListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups/{group-id}/location/{location-id}/dashboard/widgets")
public class WidgetController {
    private final CoreManagementUseCase useCase;

    @GetMapping
    public ResponseEntity<List<WidgetsListResponse>> getWidgetList(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("location-id") Long locationId) {
        List<WidgetsListResponse> responses = useCase.getWidgetList(userId, groupId, locationId);

        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{widget-id}/delete")
    public ResponseEntity<Void> deleteWidget(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("location-id") Long locationId,
            @PathVariable("widget-id") Long widgetId) {
        useCase.deleteWidget(userId, groupId, locationId, widgetId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
