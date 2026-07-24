package com.insighton.core.gateway.service.impl;

import com.insighton.core.gateway.dto.GatewayCreateRequest;
import com.insighton.core.gateway.dto.GatewayResponse;
import com.insighton.core.gateway.dto.GatewayUpdateRequest;
import com.insighton.core.gateway.entity.Gateway;
import com.insighton.core.gateway.exception.GatewayAccessDeniedException;
import com.insighton.core.gateway.exception.GatewayNotFoundException;
import com.insighton.core.gateway.repository.GatewayRepository;
import com.insighton.core.gateway.service.GatewayService;
import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groupmember.repository.GroupMembersRepository;
import com.insighton.core.mqtt.connection.DynamicMqttGatewayManager;
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
    private final DynamicMqttGatewayManager gatewayManager;
    private final GroupMembersRepository groupMembersRepository;

    @Override
    public GatewayResponse create(Long userId, GatewayCreateRequest request) {
        validateConnectionConfig(request.connectionConfig());
        requireManagerRole(userId, request.groupsId());

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

    @Override
    public GatewayResponse getById(Long userId, Long gatewayId) {
        Gateway gateway = gatewayRepository.findByGatewayId(gatewayId)
                .orElseThrow(() -> new GatewayNotFoundException(gatewayId));

        requireGroupMembership(userId, gateway.getGroupId());

        return GatewayResponse.from(gateway);
    }

    @Override
    public List<GatewayResponse> getAllByGroupId(Long userId, Long groupId) {
        requireGroupMembership(userId, groupId);

        return gatewayRepository.findAllByGroupId(groupId).stream()
                .map(GatewayResponse::from)
                .toList();
    }

    @Override
    public List<GatewayResponse> getAll(String userRole) {
        if(!Objects.equals(userRole, "ADMIN")) {
            throw new GatewayAccessDeniedException("관리자만 접근 가능합니다.");
        }

        return gatewayRepository.findAll().stream()
                .map(GatewayResponse::from)
                .toList();
    }

    @Transactional
    @Override
    public void update(Long userId, Long gatewayId, GatewayUpdateRequest request) {
        validateConnectionConfig(request.connectionConfig());

        Gateway gateway = gatewayRepository.findByGatewayId(gatewayId)
                .orElseThrow(() -> new GatewayNotFoundException(gatewayId));

        requireManagerRole(userId, gateway.getGroupId());

        gateway.update(request.name(), request.protocolType(), request.connectionConfig());
    }

    @Transactional
    @Override
    public void delete(Long userId, Long gatewayId) {
        Gateway gateway = gatewayRepository.findByGatewayId(gatewayId)
                .orElseThrow(() -> new GatewayNotFoundException(gatewayId));

        requireManagerRole(userId, gateway.getGroupId());

        gatewayRepository.deleteById(gatewayId);
        gatewayManager.unregisterGateway(gatewayId);
    }

    private void validateConnectionConfig(Map<String, Object> connectionConfig) {
        Object brokerUrls = connectionConfig.get("brokerUrls");
        if (!(brokerUrls instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("connection_config에 brokerUrls(문자열 배열)가 필요합니다.");
        }
    }


     // 쓰기(생성/수정/삭제)용 — MANAGER/SUPER_MANAGER만 허용, 호출자 소속 그룹과 대상 그룹이 같은지도 확인
    private void requireManagerRole(Long userId, Long groupsId) {
        GroupMembers groupMember = groupMembersRepository.findByUserId(userId)
                .orElseThrow(() -> new GatewayAccessDeniedException("소속된 그룹이 없습니다."));

        if (!groupMember.getGroups().getGroupId().equals(groupsId)) {
            throw new GatewayAccessDeniedException("다른 그룹의 리소스입니다.");
        }
        if (groupMember.getGroupRole() == GroupMembers.GroupRole.MEMBER) {
            throw new GatewayAccessDeniedException("게이트웨이 관리 권한이 없습니다.");
        }
    }

    // 조회용 — 그룹 소속만 확인, role은 무관(MEMBER도 허용)
    private void requireGroupMembership(Long userId, Long groupsId) {
        GroupMembers groupMember = groupMembersRepository.findByUserId(userId)
                .orElseThrow(() -> new GatewayAccessDeniedException("소속된 그룹이 없습니다."));
        if (!groupMember.getGroups().getGroupId().equals(groupsId)) {
            throw new GatewayAccessDeniedException("다른 그룹의 리소스입니다.");
        }
    }
}
