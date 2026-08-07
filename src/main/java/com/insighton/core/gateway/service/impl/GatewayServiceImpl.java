package com.insighton.core.gateway.service.impl;

import com.insighton.core.gateway.dto.GatewayCreateRequest;
import com.insighton.core.gateway.dto.GatewayResponse;
import com.insighton.core.gateway.dto.GatewayUpdateRequest;
import com.insighton.core.gateway.entity.Gateway;
import com.insighton.core.gateway.event.GatewayBrokerChangedEvent;
import com.insighton.core.gateway.event.GatewayDeletedEvent;
import com.insighton.core.gateway.exception.GatewayAccessDeniedException;
import com.insighton.core.gateway.exception.GatewayNotFoundException;
import com.insighton.core.gateway.repository.GatewayRepository;
import com.insighton.core.gateway.service.GatewayService;
import com.insighton.core.groupmember.entity.GroupMember;
import com.insighton.core.groupmember.repository.GroupMemberRepository;
import com.insighton.core.mqtt.cache.SensorLookupCacheService;
import com.insighton.core.sensorattributes.repository.SensorAttributeRepository;
import com.insighton.core.sensors.entity.Sensor;
import com.insighton.core.sensors.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GatewayServiceImpl implements GatewayService {

    private final GatewayRepository gatewayRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SensorRepository sensorRepository;
    private final SensorAttributeRepository sensorAttributeRepository;
    private final SensorLookupCacheService sensorLookupCacheService;
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
        if (!Objects.equals(userRole, "ADMIN")) {
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

        boolean brokerChanged = !Objects.equals(
                gateway.getConnectionConfig().get("brokerUrls"),
                request.connectionConfig().get("brokerUrls")
        );

        gateway.update(request.name(), request.protocolType(), request.connectionConfig());

        if(brokerChanged) {
            purgeSensorOf(gateway);
            eventPublisher.publishEvent(new GatewayBrokerChangedEvent(gatewayId));
        }
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
     *
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
        GroupMember groupMember = groupMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new GatewayAccessDeniedException("소속된 그룹이 없습니다."));

        if (!groupMember.getGroup().getGroupId().equals(groupsId)) {
            throw new GatewayAccessDeniedException("다른 그룹의 리소스입니다.");
        }
        if (groupMember.getGroupRole() == GroupMember.GroupRole.MEMBER) {
            throw new GatewayAccessDeniedException("게이트웨이 관리 권한이 없습니다.");
        }
    }

    // 조회용 — 그룹 소속만 확인, role은 무관(MEMBER도 허용)
    private void requireGroupMembership(Long userId, Long groupsId) {
        GroupMember groupMember = groupMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new GatewayAccessDeniedException("소속된 그룹이 없습니다."));
        if (!groupMember.getGroup().getGroupId().equals(groupsId)) {
            throw new GatewayAccessDeniedException("다른 그룹의 리소스입니다.");
        }
    }


    /**
     * 브로커 주소가 바뀌면 물리적으로 다른 브로커/네트워크를 가리키게 되어 기존 센서의
     * devEui 매핑이 더 이상 유효하지 않으므로, 해당 게이트웨이 소속 센서와 속성을 전부 삭제한다.
     * 캐시(Caffeine/Redis)도 함께 비우지 않으면 DB에 없는 센서를 캐시가 계속 들고 있어
     * 다음 패킷에서 삭제된 sensorId로 RabbitMQ까지 발행되는 정합성 문제가 생긴다.
     */
    private void purgeSensorOf(Gateway gateway) {
        List<Sensor> sensors = sensorRepository.findByGatewayGatewayId(gateway.getGatewayId());

        if(sensors.isEmpty()) {
            return;
        }

        List<Long> sensorIds = sensors.stream()
                .map(Sensor::getSensorId)
                .toList();

        sensorAttributeRepository.deleteAllBySensorSensorIdIn(sensorIds);
        sensorRepository.deleteAll(sensors);

        sensors.stream()
                .map(Sensor::getSensorEui)
                .filter(Objects::nonNull)
                .forEach(sensorLookupCacheService::evict);

        log.info("게이트웨이 {} 브로커 주소 변경 — 소속 센서 {}건 삭제 및 캐시 정리",
                gateway.getGatewayId(), sensors.size());
    }
}
