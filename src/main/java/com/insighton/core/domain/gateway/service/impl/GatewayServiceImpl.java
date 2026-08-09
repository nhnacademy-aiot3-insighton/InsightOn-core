package com.insighton.core.domain.gateway.service.impl;

import com.insighton.core.domain.gateway.dto.GatewayCreateRequest;
import com.insighton.core.domain.gateway.dto.GatewayResponse;
import com.insighton.core.domain.gateway.dto.GatewayUpdateRequest;
import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.gateway.event.GatewayBrokerChangedEvent;
import com.insighton.core.domain.gateway.event.GatewayDeletedEvent;
import com.insighton.core.domain.gateway.exception.GatewayAccessDeniedException;
import com.insighton.core.domain.gateway.exception.GatewayAlreadyExistsException;
import com.insighton.core.domain.gateway.exception.GatewayNotFoundException;
import com.insighton.core.domain.gateway.exception.InvalidGatewayConnectionConfigException;
import com.insighton.core.domain.gateway.exception.InvalidGatewayValueException;
import com.insighton.core.domain.gateway.repository.GatewayRepository;
import com.insighton.core.domain.gateway.service.GatewayService;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.repository.GroupMemberRepository;
import com.insighton.core.adapter.mqtt.cache.SensorLookupCacheService;
import com.insighton.core.domain.sensorattributes.repository.SensorAttributeRepository;
import com.insighton.core.domain.sensors.entity.Sensor;
import com.insighton.core.domain.sensors.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

        // group_id가 unique 제약이라(1group:1gateway) 사전 체크 없이 save()하면
        // DataIntegrityViolationException(500, 원인 불명)이 그대로 노출됨
        if (gatewayRepository.findByGroupId(request.groupsId()).isPresent()) {
            throw new GatewayAlreadyExistsException("이미 게이트웨이가 등록된 그룹입니다. groupId=" + request.groupsId());
        }

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

        GroupMember groupMember = requireGroupMembership(userId, gateway.getGroupId());

        return GatewayResponse.from(gateway, !groupMember.isMember());
    }

    @Override
    public GatewayResponse getByGroupId(Long userId, Long groupId) {
        GroupMember groupMember = requireGroupMembership(userId, groupId);

        Gateway gateway = gatewayRepository.findByGroupId(groupId)
                .orElseThrow(() -> GatewayNotFoundException.byGroupId(groupId));

        return GatewayResponse.from(gateway, !groupMember.isMember());
    }

    @Override
    public Page<GatewayResponse> getAll(String userRole, Pageable pageable) {
        if (!Objects.equals(userRole, "ADMIN")) {
            throw new GatewayAccessDeniedException("관리자만 접근 가능합니다.");
        }

        return gatewayRepository.findAll(pageable)
                .map(GatewayResponse::from);
    }

    @Transactional
    @Override
    public void update(Long userId, Long gatewayId, GatewayUpdateRequest request) {
        Gateway gateway = gatewayRepository.findByGatewayId(gatewayId)
                .orElseThrow(() -> new GatewayNotFoundException(gatewayId));

        requireManagerRole(userId, gateway.getGroupId());

        if (request.name() != null && request.name().isBlank()) {
            throw new InvalidGatewayValueException("name은 빈 값일 수 없습니다.");
        }

        boolean protocolChanging = request.protocolType() != null
                && request.protocolType() != gateway.getProtocolType();

        if (protocolChanging && request.connectionConfig() == null) {
            // 프로토콜이 바뀌면 요구하는 connection_config 형태 자체가 달라지므로,
            // 옛 config를 그대로 들고 가면 다음 MQTT 연결 시도에서 형식 오류로 터짐
            throw new InvalidGatewayValueException(
                    "protocolType을 변경하려면 connectionConfig도 함께 제공해야 합니다.");
        }

        if (request.connectionConfig() != null) {
            validateConnectionConfig(request.connectionConfig());
        }

        boolean brokerChanged = request.connectionConfig() != null && !Objects.equals(
                gateway.getConnectionConfig().get("brokerUrls"),
                request.connectionConfig().get("brokerUrls")
        );

        gateway.update(request.name(), request.protocolType(), request.connectionConfig());

        if (brokerChanged) {
            purgeSensorOf(gateway, "브로커 주소 변경");
            eventPublisher.publishEvent(new GatewayBrokerChangedEvent(gatewayId));
        }
    }

    @Transactional
    @Override
    public void delete(Long userId, Long gatewayId) {
        Gateway gateway = gatewayRepository.findByGatewayId(gatewayId)
                .orElseThrow(() -> new GatewayNotFoundException(gatewayId));

        requireManagerRole(userId, gateway.getGroupId());

        // sensors.gateway_id가 NOT NULL FK라, 소속 센서를 먼저 정리하지 않으면
        // 삭제 시점에 DataIntegrityViolationException으로 실패함
        purgeSensorOf(gateway, "게이트웨이 삭제");
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
            purgeSensorOf(gateway, "게이트웨이 삭제(그룹 삭제 cascade)");
            gatewayRepository.deleteById(gateway.getGatewayId());

            // 로그/이벤트를 실제 삭제 성공 뒤로 옮김 — 삭제가 예외로 실패해 트랜잭션이 롤백되면
            // 이 줄까지 도달하지 않으므로, 로그에 "삭제"가 찍혔다는 건 실제로 삭제됐다는 뜻이 됨
            log.info("gateway 삭제 - gatewayId: {}, groupId: {}", gateway.getGatewayId(), groupId);
            eventPublisher.publishEvent(new GatewayDeletedEvent(gateway.getGatewayId()));
        });
    }

    private void validateConnectionConfig(Map<String, Object> connectionConfig) {
        Object brokerUrls = connectionConfig.get("brokerUrls");
        if (!(brokerUrls instanceof List<?> list) || list.isEmpty()) {
            throw new InvalidGatewayConnectionConfigException("connection_config에 brokerUrls(문자열 배열)가 필요합니다.");
        }

        // topics 자체는 생략 가능(생략 시 MqttGatewayConnectionInfo.from()에서 ChirpStack 기본 토픽으로
        // 대체됨). 다만 키를 명시했는데 빈 배열이면, 구독 토픽이 하나도 없는 상태로 연결만 성공해서
        // 메시지를 전혀 못 받는 조용한 장애가 되므로 여기서 미리 막음.
        Object topics = connectionConfig.get("topics");
        if (topics != null && (!(topics instanceof List<?> topicList) || topicList.isEmpty())) {
            throw new InvalidGatewayConnectionConfigException("connection_config의 topics는 비어있지 않은 배열이어야 합니다 (생략하면 기본값 사용).");
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
    private GroupMember requireGroupMembership(Long userId, Long groupsId) {
        GroupMember groupMember = groupMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new GatewayAccessDeniedException("소속된 그룹이 없습니다."));
        if (!groupMember.getGroup().getGroupId().equals(groupsId)) {
            throw new GatewayAccessDeniedException("다른 그룹의 리소스입니다.");
        }
        return groupMember;
    }


    /**
     * 게이트웨이가 삭제되거나 브로커 주소가 바뀌어 기존 센서의 devEui 매핑이 더 이상 유효하지 않을 때,
     * 해당 게이트웨이 소속 센서와 속성을 전부 삭제한다. sensors.gateway_id가 NOT NULL FK라, 게이트웨이
     * 삭제 시에는 이걸 먼저 호출하지 않으면 DataIntegrityViolationException으로 실패한다.
     * 캐시(Caffeine/Redis)도 함께 비우지 않으면 DB에 없는 센서를 캐시가 계속 들고 있어
     * 다음 패킷에서 삭제된 sensorId로 RabbitMQ까지 발행되는 정합성 문제가 생긴다.
     *
     * @param gateway 소속 센서를 정리할 게이트웨이
     * @param reason  로그에 남길 호출 이유(브로커 주소 변경/게이트웨이 삭제 등)
     */
    private void purgeSensorOf(Gateway gateway, String reason) {
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

        log.info("게이트웨이 {} {} — 소속 센서 {}건 삭제 및 캐시 정리",
                gateway.getGatewayId(), reason, sensors.size());
    }
}
