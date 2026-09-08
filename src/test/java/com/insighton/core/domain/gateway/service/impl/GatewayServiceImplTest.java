package com.insighton.core.domain.gateway.service.impl;

import com.insighton.core.adapter.mqtt.cache.SensorLookupCacheService;
import com.insighton.core.domain.gateway.dto.GatewayCreateRequest;
import com.insighton.core.domain.gateway.dto.GatewayResponse;
import com.insighton.core.domain.gateway.dto.GatewayUpdateRequest;
import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.gateway.entity.ProtocolType;
import com.insighton.core.domain.gateway.event.GatewayBrokerChangedEvent;
import com.insighton.core.domain.gateway.event.GatewayDeletedEvent;
import com.insighton.core.domain.gateway.exception.*;
import com.insighton.core.domain.gateway.repository.GatewayRepository;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.entity.GroupMember.GroupRole;
import com.insighton.core.domain.groupmember.repository.GroupMemberRepository;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.sensorattributes.repository.SensorAttributeRepository;
import com.insighton.core.domain.sensors.entity.Sensor;
import com.insighton.core.domain.sensors.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GatewayServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long GROUP_ID = 10L;
    private static final Long GATEWAY_ID = 100L;

    @Mock
    private GatewayRepository gatewayRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private SensorRepository sensorRepository;
    @Mock
    private SensorAttributeRepository sensorAttributeRepository;
    @Mock
    private SensorLookupCacheService sensorLookupCacheService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private GatewayServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GatewayServiceImpl(gatewayRepository, groupMemberRepository, sensorRepository,
                sensorAttributeRepository, sensorLookupCacheService, eventPublisher);
    }

    private Gateway gateway(Map<String, Object> connectionConfig) {
        Gateway gateway = Gateway.builder()
                .groupsId(GROUP_ID)
                .name("gateway-1")
                .protocolType(ProtocolType.MQTT)
                .connectionConfig(connectionConfig)
                .build();
        ReflectionTestUtils.setField(gateway, "gatewayId", GATEWAY_ID);
        return gateway;
    }

    private GroupMember groupMember(Long groupId, GroupRole role) {
        Group group = Group.builder().name("group").build();
        ReflectionTestUtils.setField(group, "groupId", groupId);
        return GroupMember.builder().group(group).userId(USER_ID).groupRole(role).build();
    }

    private Map<String, Object> validConfig() {
        return Map.of("brokerUrls", List.of("tcp://localhost:1883"));
    }

    // ===== create =====

    @Test
    void create_정상_생성() {
        GatewayCreateRequest request = new GatewayCreateRequest(GROUP_ID, "gw", ProtocolType.MQTT, validConfig());
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(GROUP_ID, GroupRole.MANAGER)));
        given(gatewayRepository.findByGroupId(GROUP_ID)).willReturn(Optional.empty());
        given(gatewayRepository.save(any())).willAnswer(inv -> {
            Gateway g = inv.getArgument(0);
            ReflectionTestUtils.setField(g, "gatewayId", GATEWAY_ID);
            return g;
        });

        GatewayResponse response = service.create(USER_ID, request);

        assertThat(response.id()).isEqualTo(GATEWAY_ID);
        assertThat(response.name()).isEqualTo("gw");
    }

    @Test
    void create_brokerUrls_없으면_예외() {
        GatewayCreateRequest request = new GatewayCreateRequest(GROUP_ID, "gw", ProtocolType.MQTT, Map.of());

        assertThatThrownBy(() -> service.create(USER_ID, request))
                .isInstanceOf(InvalidGatewayConnectionConfigException.class);
        verify(groupMemberRepository, never()).findByUserId(any());
    }

    @Test
    void create_topics가_빈_배열이면_예외() {
        Map<String, Object> config = Map.of(
                "brokerUrls", List.of("tcp://localhost:1883"),
                "topics", List.of()
        );
        GatewayCreateRequest request = new GatewayCreateRequest(GROUP_ID, "gw", ProtocolType.MQTT, config);

        assertThatThrownBy(() -> service.create(USER_ID, request))
                .isInstanceOf(InvalidGatewayConnectionConfigException.class);
    }

    @Test
    void create_소속_그룹이_없으면_예외() {
        GatewayCreateRequest request = new GatewayCreateRequest(GROUP_ID, "gw", ProtocolType.MQTT, validConfig());
        given(groupMemberRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(USER_ID, request))
                .isInstanceOf(GatewayAccessDeniedException.class);
    }

    @Test
    void create_다른_그룹_리소스면_예외() {
        GatewayCreateRequest request = new GatewayCreateRequest(GROUP_ID, "gw", ProtocolType.MQTT, validConfig());
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(999L, GroupRole.MANAGER)));

        assertThatThrownBy(() -> service.create(USER_ID, request))
                .isInstanceOf(GatewayAccessDeniedException.class);
    }

    @Test
    void create_MEMBER_권한이면_예외() {
        GatewayCreateRequest request = new GatewayCreateRequest(GROUP_ID, "gw", ProtocolType.MQTT, validConfig());
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(GROUP_ID, GroupRole.MEMBER)));

        assertThatThrownBy(() -> service.create(USER_ID, request))
                .isInstanceOf(GatewayAccessDeniedException.class);
    }

    @Test
    void create_이미_게이트웨이가_있는_그룹이면_예외() {
        GatewayCreateRequest request = new GatewayCreateRequest(GROUP_ID, "gw", ProtocolType.MQTT, validConfig());
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(GROUP_ID, GroupRole.MANAGER)));
        given(gatewayRepository.findByGroupId(GROUP_ID)).willReturn(Optional.of(gateway(validConfig())));

        assertThatThrownBy(() -> service.create(USER_ID, request))
                .isInstanceOf(GatewayAlreadyExistsException.class);
        verify(gatewayRepository, never()).save(any());
    }

    // ===== getById =====

    @Test
    void getById_MANAGER는_connectionConfig을_포함해서_받는다() {
        Gateway gw = gateway(validConfig());
        given(gatewayRepository.findByGatewayId(GATEWAY_ID)).willReturn(Optional.of(gw));
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(GROUP_ID, GroupRole.MANAGER)));

        GatewayResponse response = service.getById(USER_ID, GATEWAY_ID);

        assertThat(response.connectionConfig()).isNotNull();
    }

    @Test
    void getById_MEMBER는_connectionConfig을_받지_못한다() {
        Gateway gw = gateway(validConfig());
        given(gatewayRepository.findByGatewayId(GATEWAY_ID)).willReturn(Optional.of(gw));
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(GROUP_ID, GroupRole.MEMBER)));

        GatewayResponse response = service.getById(USER_ID, GATEWAY_ID);

        assertThat(response.connectionConfig()).isNull();
    }

    @Test
    void getById_없으면_예외() {
        given(gatewayRepository.findByGatewayId(GATEWAY_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(USER_ID, GATEWAY_ID))
                .isInstanceOf(GatewayNotFoundException.class);
    }

    @Test
    void getById_다른_그룹_소속이면_예외() {
        Gateway gw = gateway(validConfig());
        given(gatewayRepository.findByGatewayId(GATEWAY_ID)).willReturn(Optional.of(gw));
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(999L, GroupRole.MEMBER)));

        assertThatThrownBy(() -> service.getById(USER_ID, GATEWAY_ID))
                .isInstanceOf(GatewayAccessDeniedException.class);
    }

    // ===== getByGroupId =====

    @Test
    void getByGroupId_정상_조회() {
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(GROUP_ID, GroupRole.MEMBER)));
        given(gatewayRepository.findByGroupId(GROUP_ID)).willReturn(Optional.of(gateway(validConfig())));

        GatewayResponse response = service.getByGroupId(USER_ID, GROUP_ID);

        assertThat(response.groupsId()).isEqualTo(GROUP_ID);
    }

    @Test
    void getByGroupId_없으면_예외() {
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(GROUP_ID, GroupRole.MEMBER)));
        given(gatewayRepository.findByGroupId(GROUP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByGroupId(USER_ID, GROUP_ID))
                .isInstanceOf(GatewayNotFoundException.class);
    }

    // ===== getAll =====

    @Test
    void getAll_ADMIN이_아니면_예외() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.getAll("USER", pageRequest))
                .isInstanceOf(GatewayAccessDeniedException.class);
        verify(gatewayRepository, never()).findAll(any(PageRequest.class));
    }

    // ===== update =====

    @Test
    void update_이름만_바꾸면_브로커_변경_이벤트는_발행되지_않는다() {
        Gateway gw = gateway(validConfig());
        given(gatewayRepository.findByGatewayId(GATEWAY_ID)).willReturn(Optional.of(gw));
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(GROUP_ID, GroupRole.MANAGER)));

        service.update(USER_ID, GATEWAY_ID, new GatewayUpdateRequest("new-name", null, null));

        assertThat(gw.getName()).isEqualTo("new-name");
        verify(eventPublisher, never()).publishEvent(any());
        verify(sensorRepository, never()).findByGatewayGatewayId(any());
    }

    @Test
    void update_이름이_빈값이면_예외() {
        Gateway gw = gateway(validConfig());
        given(gatewayRepository.findByGatewayId(GATEWAY_ID)).willReturn(Optional.of(gw));
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(GROUP_ID, GroupRole.MANAGER)));

        GatewayUpdateRequest request = new GatewayUpdateRequest("  ", null, null);

        assertThatThrownBy(() -> service.update(USER_ID, GATEWAY_ID, request))
                .isInstanceOf(InvalidGatewayValueException.class);
    }

    @Test
    void update_프로토콜_변경시_connectionConfig_없으면_예외() {
        Gateway gw = gateway(validConfig());
        given(gatewayRepository.findByGatewayId(GATEWAY_ID)).willReturn(Optional.of(gw));
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(GROUP_ID, GroupRole.MANAGER)));

        GatewayUpdateRequest request = new GatewayUpdateRequest(null, ProtocolType.MODBUS_TCP, null);

        assertThatThrownBy(() -> service.update(USER_ID, GATEWAY_ID, request))
                .isInstanceOf(InvalidGatewayValueException.class);
    }

    @Test
    void update_새_connectionConfig이_유효하지_않으면_예외() {
        Gateway gw = gateway(validConfig());
        given(gatewayRepository.findByGatewayId(GATEWAY_ID)).willReturn(Optional.of(gw));
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(GROUP_ID, GroupRole.MANAGER)));

        GatewayUpdateRequest request = new GatewayUpdateRequest(null, null, Map.of());

        assertThatThrownBy(() -> service.update(USER_ID, GATEWAY_ID, request))
                .isInstanceOf(InvalidGatewayConnectionConfigException.class);
    }

    @Test
    void update_브로커_주소가_바뀌면_센서를_정리하고_이벤트를_발행한다() {
        Gateway gw = gateway(validConfig());
        Sensor sensor = Sensor.builder().sensorId(1L).sensorEui("eui-1").build();
        given(gatewayRepository.findByGatewayId(GATEWAY_ID)).willReturn(Optional.of(gw));
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(GROUP_ID, GroupRole.MANAGER)));
        given(sensorRepository.findByGatewayGatewayId(GATEWAY_ID)).willReturn(List.of(sensor));

        Map<String, Object> newConfig = Map.of("brokerUrls", List.of("tcp://new-broker:1883"));
        service.update(USER_ID, GATEWAY_ID, new GatewayUpdateRequest(null, null, newConfig));

        verify(sensorAttributeRepository).deleteAllBySensorSensorIdIn(List.of(1L));
        verify(sensorRepository).deleteAll(List.of(sensor));
        verify(sensorLookupCacheService).evict("eui-1");
        verify(eventPublisher).publishEvent(any(GatewayBrokerChangedEvent.class));
    }

    @Test
    void update_브로커_주소가_그대로면_센서를_정리하지_않는다() {
        Gateway gw = gateway(validConfig()); // brokerUrls = tcp://localhost:1883
        given(gatewayRepository.findByGatewayId(GATEWAY_ID)).willReturn(Optional.of(gw));
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(GROUP_ID, GroupRole.MANAGER)));

        service.update(USER_ID, GATEWAY_ID, new GatewayUpdateRequest(null, null, validConfig()));

        verify(eventPublisher, never()).publishEvent(any());
        verify(sensorRepository, never()).findByGatewayGatewayId(any());
    }

    @Test
    void update_게이트웨이가_없으면_예외() {
        given(gatewayRepository.findByGatewayId(GATEWAY_ID)).willReturn(Optional.empty());

        GatewayUpdateRequest request = new GatewayUpdateRequest("n", null, null);

        assertThatThrownBy(() -> service.update(USER_ID, GATEWAY_ID, request))
                .isInstanceOf(GatewayNotFoundException.class);
    }

    @Test
    void update_권한이_없으면_예외() {
        Gateway gw = gateway(validConfig());
        given(gatewayRepository.findByGatewayId(GATEWAY_ID)).willReturn(Optional.of(gw));
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(GROUP_ID, GroupRole.MEMBER)));

        GatewayUpdateRequest request = new GatewayUpdateRequest("n", null, null);

        assertThatThrownBy(() -> service.update(USER_ID, GATEWAY_ID, request))
                .isInstanceOf(GatewayAccessDeniedException.class);
    }

    // ===== delete =====

    @Test
    void delete_정상_삭제시_센서_정리와_이벤트_발행이_함께_일어난다() {
        Gateway gw = gateway(validConfig());
        Sensor sensor = Sensor.builder().sensorId(1L).sensorEui("eui-1").build();
        given(gatewayRepository.findByGatewayId(GATEWAY_ID)).willReturn(Optional.of(gw));
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(GROUP_ID, GroupRole.MANAGER)));
        given(sensorRepository.findByGatewayGatewayId(GATEWAY_ID)).willReturn(List.of(sensor));

        service.delete(USER_ID, GATEWAY_ID);

        verify(sensorRepository).deleteAll(List.of(sensor));
        verify(gatewayRepository).deleteById(GATEWAY_ID);
        verify(eventPublisher).publishEvent(any(GatewayDeletedEvent.class));
    }

    @Test
    void delete_소속_센서가_없으면_정리_단계를_건너뛴다() {
        Gateway gw = gateway(validConfig());
        given(gatewayRepository.findByGatewayId(GATEWAY_ID)).willReturn(Optional.of(gw));
        given(groupMemberRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(groupMember(GROUP_ID, GroupRole.MANAGER)));
        given(sensorRepository.findByGatewayGatewayId(GATEWAY_ID)).willReturn(List.of());

        service.delete(USER_ID, GATEWAY_ID);

        verify(sensorAttributeRepository, never()).deleteAllBySensorSensorIdIn(any());
        verify(sensorRepository, never()).deleteAll(any());
        verify(gatewayRepository).deleteById(GATEWAY_ID);
    }

    @Test
    void delete_게이트웨이가_없으면_예외() {
        given(gatewayRepository.findByGatewayId(GATEWAY_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER_ID, GATEWAY_ID))
                .isInstanceOf(GatewayNotFoundException.class);
        verify(gatewayRepository, never()).deleteById(any());
    }

    @Test
    void delete_권한이_없으면_예외() {
        Gateway gw = gateway(validConfig());
        given(gatewayRepository.findByGatewayId(GATEWAY_ID)).willReturn(Optional.of(gw));
        given(groupMemberRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER_ID, GATEWAY_ID))
                .isInstanceOf(GatewayAccessDeniedException.class);
        verify(gatewayRepository, never()).deleteById(any());
    }

    // ===== deleteByGroupId =====

    @Test
    void deleteByGroupId_게이트웨이가_있으면_삭제하고_이벤트를_발행한다() {
        Gateway gw = gateway(validConfig());
        given(gatewayRepository.findByGroupId(GROUP_ID)).willReturn(Optional.of(gw));
        given(sensorRepository.findByGatewayGatewayId(GATEWAY_ID)).willReturn(List.of());

        service.deleteByGroupId(GROUP_ID);

        verify(gatewayRepository).deleteById(GATEWAY_ID);
        verify(eventPublisher).publishEvent(any(GatewayDeletedEvent.class));
    }

    @Test
    void deleteByGroupId_게이트웨이가_없으면_아무_일도_하지_않는다() {
        given(gatewayRepository.findByGroupId(GROUP_ID)).willReturn(Optional.empty());

        service.deleteByGroupId(GROUP_ID);

        verify(gatewayRepository, never()).deleteById(anyLong());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
