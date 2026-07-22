package com.insighton.core.gateway.service;

import com.insighton.core.gateway.dto.GatewayCreateRequest;
import com.insighton.core.gateway.dto.GatewayResponse;
import com.insighton.core.gateway.dto.GatewayUpdateRequest;
import java.util.List;

public interface GatewayService {
    /**
 * 지정된 그룹에 게이트웨이를 등록한다.
 *
 * @param userId  등록 요청자의 사용자 ID
 * @param request 등록할 게이트웨이 정보
 * @return 등록된 게이트웨이 정보
 */
    GatewayResponse create(Long userId, GatewayCreateRequest request);

    /**
 * 게이트웨이 식별자로 게이트웨이 정보를 조회한다.
 *
 * @param userId    조회 요청자의 사용자 식별자
 * @param gatewayId 조회할 게이트웨이 식별자
 * @return 게이트웨이 정보
 */
    GatewayResponse getById(Long userId, Long gatewayId);

    /**
 * 사용자가 소속된 그룹의 모든 게이트웨이를 조회한다.
 *
 * @param userId  조회 요청자의 사용자 ID
 * @param groupId 조회할 그룹 ID
 * @return 해당 그룹의 게이트웨이 목록
 */
    List<GatewayResponse> getAllByGroupId(Long userId, Long groupId);

    /**
 * 관리자 권한으로 모든 게이트웨이를 조회한다.
 *
 * @param userRole 요청자의 사용자 역할
 * @return 사용자 역할이 ADMIN인 경우 전체 게이트웨이 목록
 */
    List<GatewayResponse> getAll(String userRole);

    /**
 * 게이트웨이 정보를 수정한다.
 *
 * @param userId   요청자의 사용자 ID
 * @param gatewayId 수정할 게이트웨이의 ID
 * @param request  게이트웨이 수정 정보
 */
    void update(Long userId, Long gatewayId, GatewayUpdateRequest request);

    /**
 * 게이트웨이를 삭제한다.
 *
 * @param userId    요청자의 사용자 ID
 * @param gatewayId 삭제할 게이트웨이 ID
 */
    void delete(Long userId, Long gatewayId);

}
