package com.insighton.core.domain.gateway.service;

import com.insighton.core.domain.gateway.dto.GatewayCreateRequest;
import com.insighton.core.domain.gateway.dto.GatewayResponse;
import com.insighton.core.domain.gateway.dto.GatewayUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GatewayService {
    //gateway 등록 (요청자가 groupsId에 대해 MANAGER/SUPER_MANAGER인지 확인)
    GatewayResponse create(Long userId, GatewayCreateRequest request);

    //gateway 조회 (요청자가 해당 게이트웨이의 그룹 소속인지만 확인, MEMBER도 허용 — MANAGER 이상이면 connectionConfig 포함)
    GatewayResponse getById(Long userId, Long gatewayId);

    //group의 게이트웨이 조회 (1group:1gateway, 요청자가 그 그룹 소속인지만 확인, MEMBER도 허용 — MANAGER 이상이면 connectionConfig 포함)
    GatewayResponse getByGroupId(Long userId, Long groupId);

    //관리자용 전체 조회 (X-User-Role == ADMIN만 허용, 그룹 소속과 무관)
    Page<GatewayResponse> getAll(String userRole, Pageable pageable);

    //gateway 수정 (요청자가 해당 게이트웨이의 그룹에 대해 MANAGER/SUPER_MANAGER인지 확인)
    void update(Long userId, Long gatewayId, GatewayUpdateRequest request);

    //gateway 삭제 (요청자가 해당 게이트웨이의 그룹에 대해 MANAGER/SUPER_MANAGER인지 확인)
    void delete(Long userId, Long gatewayId);

    //gateway 삭제 group 삭제용
    void deleteByGroupId(Long groupId);
}
