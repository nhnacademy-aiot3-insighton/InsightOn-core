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

    @Override
    public GatewayResponse getById(Long userId, Long gatewayId) {
        requireManagerRole(userId, null);

        Gateway gateway = gatewayRepository.findByGatewayId(gatewayId)
                .orElseThrow(() -> new GatewayNotFoundException(gatewayId));

        return GatewayResponse.from(gateway);
    }

    @Override
    public List<GatewayResponse> getAllByGroupId(Long userId, Long groupId) {
        requireManagerRole(userId, null);

        return gatewayRepository.findAllByGroupsId(groupId).stream()
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
        requireManagerRole(userId, null);

        Gateway gateway = gatewayRepository.findByGatewayId(gatewayId)
                .orElseThrow(() -> new GatewayNotFoundException(gatewayId));

        gateway.update(request.name(), request.protocolType(), request.connectionConfig());
    }

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

    private void validateConnectionConfig(Map<String, Object> connectionConfig) {
        Object brokerUrls = connectionConfig.get("brokerUrls");
        if (!(brokerUrls instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("connection_config에 brokerUrls(문자열 배열)가 필요합니다.");
        }
    }

    private void requireManagerRole(Long userId, Long groupId) {
        //TODO: groupMemberRepository ROLE MANAGER 체크
    }
}
