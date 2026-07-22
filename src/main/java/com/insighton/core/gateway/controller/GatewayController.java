package com.insighton.core.gateway.controller;

import com.insighton.core.gateway.dto.GatewayCreateRequest;
import com.insighton.core.gateway.dto.GatewayResponse;
import com.insighton.core.gateway.dto.GatewayUpdateRequest;
import com.insighton.core.gateway.service.GatewayService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    /**
     * 게이트웨이를 생성합니다.
     *
     * @param userId  요청한 사용자의 식별자
     * @param request 게이트웨이 생성 요청 정보
     * @return 생성된 게이트웨이 정보
     */
    @PostMapping
    public ResponseEntity<GatewayResponse> create(@RequestHeader("X-User-Id") Long userId,
                                                  @Valid @RequestBody GatewayCreateRequest request) {
        return ResponseEntity.ok(gatewayService.create(userId, request));
    }

    /**
     * 사용자 권한으로 지정된 게이트웨이를 조회합니다.
     *
     * @param userId 게이트웨이 조회를 요청한 사용자 ID
     * @param gatewayId 조회할 게이트웨이 ID
     * @return 조회된 게이트웨이 정보
     */
    @GetMapping("/{gatewayId}")
    public ResponseEntity<GatewayResponse> getById(@RequestHeader("X-User-Id") Long userId,
                                                   @PathVariable Long gatewayId) {
        return ResponseEntity.ok(gatewayService.getById(userId, gatewayId));
    }

    /**
     * 그룹에 속한 게이트웨이 목록을 조회합니다.
     *
     * @param userId  요청한 사용자의 식별자
     * @param groupId 조회할 그룹의 식별자
     * @return 그룹에 속한 게이트웨이 응답 목록
     */
    @GetMapping
    public ResponseEntity<List<GatewayResponse>> getAllByGroupId(@RequestHeader("X-User-Id") Long userId,
                                                                 @RequestParam Long groupId) {
        return ResponseEntity.ok(gatewayService.getAllByGroupId(userId, groupId));
    }

    /**
     * 관리자 권한으로 모든 게이트웨이를 조회합니다.
     *
     * @param userRole 요청 헤더의 사용자 역할
     * @return 게이트웨이 목록
     */
    @GetMapping("/admin")
    public ResponseEntity<List<GatewayResponse>> getAll(@RequestHeader("X-User-Role") String userRole) {
        return ResponseEntity.ok(gatewayService.getAll(userRole));
    }

    /**
     * 게이트웨이 정보를 수정합니다.
     *
     * @param userId  요청 사용자의 식별자
     * @param gatewayId 수정할 게이트웨이의 식별자
     * @param request 게이트웨이 수정 정보
     */
    @PutMapping("/{gatewayId}")
    public ResponseEntity<Void> update(@RequestHeader("X-User-Id") Long userId,
                                                  @PathVariable Long gatewayId,
                                                  @Valid @RequestBody GatewayUpdateRequest request) {
        gatewayService.update(userId, gatewayId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * 게이트웨이를 삭제합니다.
     *
     * @param userId    요청한 사용자의 식별자
     * @param gatewayId 삭제할 게이트웨이의 식별자
     * @return          본문이 없는 응답
     */
    @DeleteMapping("/{gatewayId}")
    public ResponseEntity<Void> delete(@RequestHeader("X-User-Id") Long userId,
                                       @PathVariable Long gatewayId) {
        gatewayService.delete(userId, gatewayId);
        return ResponseEntity.noContent().build();
    }
}
