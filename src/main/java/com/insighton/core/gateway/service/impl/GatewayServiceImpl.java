package com.insighton.core.gateway.service.impl;

import com.insighton.core.gateway.dto.GatewayCreateRequest;
import com.insighton.core.gateway.dto.GatewayResponse;
import com.insighton.core.gateway.dto.GatewayUpdateRequest;
import com.insighton.core.gateway.entity.Gateway;
import com.insighton.core.gateway.exception.GatewayAccessDeniedException;
import com.insighton.core.gateway.exception.GatewayNotFoundException;
import com.insighton.core.gateway.repository.GatewayRepository;
import com.insighton.core.gateway.service.GatewayService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GatewayServiceImpl implements GatewayService {

    private final GatewayRepository gatewayRepository;

    /**
     * 게이트웨이 생성 요청을 검증하고 저장된 게이트웨이 정보를 반환한다.
     *
     * @param userId  게이트웨이를 생성하는 사용자 식별자
     * @param request 게이트웨이 생성 정보
     * @return 저장된 게이트웨이 응답
     * @throws IllegalArgumentException 연결 설정에 유효한 brokerUrls가 없을 때
     */
    @Override
    public GatewayResponse create(Long userId, GatewayCreateRequest request) {
        validateConnectionConfig(request.connectionConfig());
        requireManagerRole(userId, null);

        Gateway newGateway = Gateway.builder()
                .groupsId(request.groupsId())
                .gatewayUid(request.gatewayUid())
                .name(request.name())
                .protocolType(request.protocolType())
                .connectionConfig(request.connectionConfig())
                .build();

        Gateway gateway = gatewayRepository.save(newGateway);

        return GatewayResponse.from(gateway);
    }

    /**
     * 지정한 식별자의 게이트웨이를 조회한다.
     *
     * @param userId 게이트웨이 조회를 요청한 사용자 식별자
     * @param gatewayId 조회할 게이트웨이 식별자
     * @return 조회한 게이트웨이 응답
     * @throws GatewayNotFoundException 지정한 게이트웨이가 존재하지 않는 경우
     */
    @Override
    public GatewayResponse getById(Long userId, Long gatewayId) {
        requireManagerRole(userId, null);

        Gateway gateway = gatewayRepository.findByGatewayId(gatewayId)
                .orElseThrow(() -> new GatewayNotFoundException(gatewayId));

        return GatewayResponse.from(gateway);
    }

    /**
     * 그룹에 속한 모든 게이트웨이를 조회한다.
     *
     * @param userId  게이트웨이 조회를 요청한 사용자 ID
     * @param groupId 게이트웨이를 조회할 그룹 ID
     * @return 그룹에 속한 게이트웨이 응답 목록
     */
    @Override
    public List<GatewayResponse> getAllByGroupId(Long userId, Long groupId) {
        requireManagerRole(userId, null);

        return gatewayRepository.findAllByGroupsId(groupId).stream()
                .map(GatewayResponse::from)
                .toList();
    }

    /**
     * 관리자 권한으로 모든 게이트웨이를 조회합니다.
     *
     * @param userRole 요청자의 사용자 역할
     * @return 조회된 모든 게이트웨이 응답 목록
     * @throws GatewayAccessDeniedException 사용자 역할이 관리자 권한이 아닌 경우
     */
    @Override
    public List<GatewayResponse> getAll(String userRole) {
        if(!Objects.equals(userRole, "ADMIN")) {
            throw new GatewayAccessDeniedException("관리자만 접근 가능합니다.");
        }

        return gatewayRepository.findAll().stream()
                .map(GatewayResponse::from)
                .toList();
    }

    /**
     * 게이트웨이의 이름, 프로토콜 유형 및 연결 설정을 수정한다.
     *
     * @param userId    수정 요청을 수행하는 사용자 ID
     * @param gatewayId 수정할 게이트웨이 ID
     * @param request   수정할 게이트웨이 정보
     * @throws IllegalArgumentException 연결 설정에 유효한 brokerUrls가 없을 때
     * @throws GatewayNotFoundException 지정한 게이트웨이를 찾을 수 없을 때
     */
    @Transactional
    @Override
    public void update(Long userId, Long gatewayId, GatewayUpdateRequest request) {
        validateConnectionConfig(request.connectionConfig());
        requireManagerRole(userId, null);

        Gateway gateway = gatewayRepository.findByGatewayId(gatewayId)
                .orElseThrow(() -> new GatewayNotFoundException(gatewayId));

        gateway.update(request.name(), request.protocolType(), request.connectionConfig());
    }

    /**
     * 게이트웨이를 삭제한다.
     *
     * @param userId 삭제를 요청한 사용자 ID
     * @param gatewayId 삭제할 게이트웨이 ID
     * @throws GatewayNotFoundException 지정한 게이트웨이를 찾을 수 없는 경우
     */
    @Transactional
    @Override
    public void delete(Long userId, Long gatewayId) {
        //TODO: feature/mqtt 머지 후 DynamicMqttGatewayManager.unregisterGateway(gatewayId) 호출 추가
        requireManagerRole(userId, null);

        if(!gatewayRepository.existsById(gatewayId)) {
            throw new GatewayNotFoundException(gatewayId);
        }

        gatewayRepository.deleteById(gatewayId);
    }

    /**
     * 연결 설정에 비어 있지 않은 브로커 URL 목록이 포함되어 있는지 검증합니다.
     *
     * @param connectionConfig 연결 설정
     * @throws IllegalArgumentException 브로커 URL 목록이 없거나 비어 있는 경우
     */
    private void validateConnectionConfig(Map<String, Object> connectionConfig) {
        Object brokerUrls = connectionConfig.get("brokerUrls");
        if (!(brokerUrls instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("connection_config에 brokerUrls(문자열 배열)가 필요합니다.");
        }
    }

    /**
     * 그룹 내 사용자의 관리자 권한을 확인합니다.
     *
     * @param userId  권한을 확인할 사용자 ID
     * @param groupId 권한을 확인할 그룹 ID
     */
    private void requireManagerRole(Long userId, Long groupId) {
        //TODO: groupMemberRepository ROLE MANAGER 체크
    }
}
