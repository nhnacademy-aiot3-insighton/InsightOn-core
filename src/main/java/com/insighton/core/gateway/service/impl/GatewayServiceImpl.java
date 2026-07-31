package com.insighton.core.gateway.service.impl;

import com.insighton.core.gateway.dto.GatewayCreateRequest;
import com.insighton.core.gateway.dto.GatewayResponse;
import com.insighton.core.gateway.dto.GatewayUpdateRequest;
import com.insighton.core.gateway.entity.Gateway;
import com.insighton.core.gateway.exception.GatewayAccessDeniedException;
import com.insighton.core.gateway.event.GatewayDeletedEvent;
import com.insighton.core.gateway.exception.GatewayNotFoundException;
import com.insighton.core.gateway.repository.GatewayRepository;
import com.insighton.core.gateway.service.GatewayService;
import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groupmember.repository.GroupMembersRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GatewayServiceImpl implements GatewayService {

    private final GatewayRepository gatewayRepository;
    private final GroupMembersRepository groupMembersRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public GatewayResponse create(Long userId, GatewayCreateRequest request) {
        validateConnectionConfig(request.connectionConfig());
        requireManagerRole(userId, request.groupsId());

        Gateway newGateway = Gateway.builder()
                .groupsId(request.groupsId())
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
    public GatewayResponse getByGroupId(Long userId, Long groupId) {
        requireGroupMembership(userId, groupId);

        Gateway gateway = gatewayRepository.findByGroupId(groupId)
                .orElseThrow(() -> new GatewayNotFoundException("게이트웨이를 찾을 수 없습니다. groupId=" + groupId));

        return GatewayResponse.from(gateway);
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
        eventPublisher.publishEvent(new GatewayDeletedEvent(gatewayId));
    }


    /**
     * group 삭제 시 호출용
     * @param groupId 그룹ID
     */
    @Transactional
    @Override
    public void deleteByGroupId(Long groupId) {
        gatewayRepository.findByGroupId(groupId).ifPresent(gateway -> {
            log.info("gateway 삭제 - gatewayId: {}, groupId: {}", gateway.getGatewayId(), groupId);
            eventPublisher.publishEvent(new GatewayDeletedEvent(gateway.getGatewayId()));
        });

        gatewayRepository.deleteByGroupId(groupId);
    }

    private void validateConnectionConfig(Map<String, Object> connectionConfig) {
        Object brokerUrls = connectionConfig.get("brokerUrls");
        if (!(brokerUrls instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("connection_config에 brokerUrls(문자열 배열)가 필요합니다.");
        }

        // topics 자체는 생략 가능(생략 시 MqttGatewayConnectionInfo.from()에서 ChirpStack 기본 토픽으로
        // 대체됨). 다만 키를 명시했는데 빈 배열이면, 구독 토픽이 하나도 없는 상태로 연결만 성공해서
        // 메시지를 전혀 못 받는 조용한 장애가 되므로 여기서 미리 막음.
        Object topics = connectionConfig.get("topics");
        if (topics != null && (!(topics instanceof List<?> topicList) || topicList.isEmpty())) {
            throw new IllegalArgumentException("connection_config의 topics는 비어있지 않은 배열이어야 합니다 (생략하면 기본값 사용).");
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
