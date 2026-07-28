package com.insighton.core.gateway.controller;

import com.insighton.core.gateway.dto.GatewayCreateRequest;
import com.insighton.core.gateway.dto.GatewayResponse;
import com.insighton.core.gateway.dto.GatewayUpdateRequest;
import com.insighton.core.gateway.service.GatewayService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * X-User-Id/X-User-Role은 API Gateway가 인증 후 붙여주는 걸 신뢰하는 헤더 기반 모델.
 * 실제 권한 검증은 GatewayService에서 처리함(Controller는 헤더 추출만 담당).
 */
@RestController
@RequestMapping("/api/gateways")
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayService gatewayService;

    @PostMapping
    public ResponseEntity<GatewayResponse> create(@RequestHeader("X-User-Id") Long userId,
                                                  @Valid @RequestBody GatewayCreateRequest request) {
        return ResponseEntity.ok(gatewayService.create(userId, request));
    }

    @GetMapping("/{gatewayId}")
    public ResponseEntity<GatewayResponse> getById(@RequestHeader("X-User-Id") Long userId,
                                                   @PathVariable Long gatewayId) {
        return ResponseEntity.ok(gatewayService.getById(userId, gatewayId));
    }

    @GetMapping
    public ResponseEntity<GatewayResponse> getByGroupId(@RequestHeader("X-User-Id") Long userId,
                                                         @RequestParam Long groupId) {
        return ResponseEntity.ok(gatewayService.getByGroupId(userId, groupId));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<GatewayResponse>> getAll(@RequestHeader("X-User-Role") String userRole) {
        return ResponseEntity.ok(gatewayService.getAll(userRole));
    }

    @PutMapping("/{gatewayId}")
    public ResponseEntity<Void> update(@RequestHeader("X-User-Id") Long userId,
                                                  @PathVariable Long gatewayId,
                                                  @Valid @RequestBody GatewayUpdateRequest request) {
        gatewayService.update(userId, gatewayId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{gatewayId}")
    public ResponseEntity<Void> delete(@RequestHeader("X-User-Id") Long userId,
                                       @PathVariable Long gatewayId) {
        gatewayService.delete(userId, gatewayId);
        return ResponseEntity.noContent().build();
    }
}
