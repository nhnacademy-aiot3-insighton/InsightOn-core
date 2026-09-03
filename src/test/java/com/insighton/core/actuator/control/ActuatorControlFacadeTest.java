package com.insighton.core.actuator.control;

import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuators.control.ActuatorControlAdapter;
import com.insighton.core.domain.actuators.control.ActuatorControlAdapterRegistry;
import com.insighton.core.domain.actuators.control.ActuatorControlCommand;
import com.insighton.core.domain.actuators.control.ActuatorControlResult;
import com.insighton.core.domain.actuators.control.ControlProvider;
import com.insighton.core.domain.actuators.entity.Actuator;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.exception.ActuatorNotFoundException;
import com.insighton.core.domain.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.domain.actuators.repository.ActuatorRepository;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.repository.LocationRepository;
import com.insighton.core.usecase.actuator.ActuatorControlFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ActuatorControlFacadeTest {

    @Mock private ActuatorRepository actuatorRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private ActuatorControlAdapterRegistry adapterRegistry;
    @Mock private ActuatorService actuatorService;
    @Mock private ActuatorControlAdapter adapter;

    @InjectMocks
    private ActuatorControlFacade facade;

    private static Map<String, Object> state(String... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    private static Actuator bound(ControlProvider provider, Map<String, Object> currentState) {
        return Actuator.builder()
                .actuatorId(1L)
                .actuatorType(ActuatorType.AIRCON)
                .currentState(currentState)
                .controlProvider(provider)
                .externalDeviceId("dev-001")
                .build();
    }

    @Test
    @DisplayName("성공 - provider에 맞는 Adapter 선택, Adapter 성공 후에만 CORE 상태·로그 저장")
    void control_성공_어댑터후_저장() {
        given(actuatorRepository.findById(1L))
                .willReturn(Optional.of(bound(ControlProvider.SMART_THINGS, state("power", "OFF"))));
        given(adapterRegistry.get(ControlProvider.SMART_THINGS)).willReturn(adapter);
        Map<String, Object> applied = state("power", "ON");
        given(adapter.control(any())).willReturn(new ActuatorControlResult(applied, "{\"ok\":true}"));

        facade.control(5L, 1L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null);

        verify(adapterRegistry).get(ControlProvider.SMART_THINGS);
        verify(actuatorService).updateActuatorState(5L, 1L, applied, ExecutedByType.RULE_ENGINE, null);
    }

    @Test
    @DisplayName("같은 AIRCON이어도 controlProvider에 따라 다른 Adapter가 선택됨")
    void control_provider별_어댑터선택() {
        given(actuatorRepository.findById(1L))
                .willReturn(Optional.of(bound(ControlProvider.LG_THINQ, state("power", "OFF"))));
        given(adapterRegistry.get(ControlProvider.LG_THINQ)).willReturn(adapter);
        given(adapter.control(any())).willReturn(new ActuatorControlResult(state("power", "ON"), null));

        facade.control(5L, 1L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null);

        verify(adapterRegistry).get(ControlProvider.LG_THINQ);
    }

    @Test
    @DisplayName("부분 요청은 기존 상태와 병합되어 Adapter에 전달됨 (deviceId/type 포함)")
    void control_상태병합() {
        given(actuatorRepository.findById(1L))
                .willReturn(Optional.of(bound(ControlProvider.SMART_THINGS, state("power", "OFF", "mode", "COOL"))));
        given(adapterRegistry.get(ControlProvider.SMART_THINGS)).willReturn(adapter);
        given(adapter.control(any())).willReturn(new ActuatorControlResult(Map.of(), null));

        facade.control(5L, 1L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null);

        ArgumentCaptor<ActuatorControlCommand> captor = ArgumentCaptor.forClass(ActuatorControlCommand.class);
        verify(adapter).control(captor.capture());
        ActuatorControlCommand cmd = captor.getValue();
        assertThat(cmd.desiredState()).containsEntry("power", "ON").containsEntry("mode", "COOL");
        assertThat(cmd.externalDeviceId()).isEqualTo("dev-001");
        assertThat(cmd.actuatorType()).isEqualTo(ActuatorType.AIRCON);
    }

    @Test
    @DisplayName("Adapter가 예외를 던지면(공급자 실패) CORE 상태·로그 저장 안 함")
    void control_어댑터실패_저장안함() {
        given(actuatorRepository.findById(1L))
                .willReturn(Optional.of(bound(ControlProvider.SMART_THINGS, state("power", "OFF"))));
        given(adapterRegistry.get(ControlProvider.SMART_THINGS)).willReturn(adapter);
        given(adapter.control(any())).willThrow(new RuntimeException("공급자 500"));

        assertThatThrownBy(() -> facade.control(5L, 1L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null))
                .isInstanceOf(RuntimeException.class);

        verify(actuatorService, never()).updateActuatorState(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("controlProvider가 없는(UNBOUND) 액추에이터는 제어 거절 - Adapter 조회조차 안 함")
    void control_UNBOUND_거절() {
        Actuator unbound = Actuator.builder()
                .actuatorId(1L).actuatorType(ActuatorType.AIRCON)
                .currentState(state("power", "OFF"))
                .build();
        given(actuatorRepository.findById(1L)).willReturn(Optional.of(unbound));

        assertThatThrownBy(() -> facade.control(5L, 1L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null))
                .isInstanceOf(InvalidActuatorValueException.class);

        verifyNoInteractions(adapterRegistry);
        verify(actuatorService, never()).updateActuatorState(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("externalDeviceId가 없으면 제어 거절")
    void control_externalDeviceId없음_거절() {
        Actuator noDeviceId = Actuator.builder()
                .actuatorId(1L).actuatorType(ActuatorType.AIRCON)
                .currentState(state("power", "OFF"))
                .controlProvider(ControlProvider.SMART_THINGS)
                .build();
        given(actuatorRepository.findById(1L)).willReturn(Optional.of(noDeviceId));

        assertThatThrownBy(() -> facade.control(5L, 1L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null))
                .isInstanceOf(InvalidActuatorValueException.class);

        verifyNoInteractions(adapterRegistry);
    }

    @Test
    @DisplayName("빈 상태값이면 조회도 하지 않고 InvalidActuatorValueException")
    void control_빈상태값_거절() {
        assertThatThrownBy(() -> facade.control(5L, 1L, Map.of(), ExecutedByType.RULE_ENGINE, null))
                .isInstanceOf(InvalidActuatorValueException.class);

        verifyNoInteractions(actuatorRepository);
    }

    @Test
    @DisplayName("없는 액추에이터면 ActuatorNotFoundException")
    void control_없는액추에이터() {
        given(actuatorRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> facade.control(5L, 999L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null))
                .isInstanceOf(ActuatorNotFoundException.class);
    }

    @Test
    @DisplayName("허용 안 된 명령 값이면 InvalidActuatorValueException - Adapter로 안 나감")
    void control_비허용값_거절() {
        given(actuatorRepository.findById(1L))
                .willReturn(Optional.of(bound(ControlProvider.SMART_THINGS, state("power", "OFF"))));

        assertThatThrownBy(() -> facade.control(5L, 1L, Map.of("power", "EXPLODE"), ExecutedByType.RULE_ENGINE, null))
                .isInstanceOf(InvalidActuatorValueException.class);

        verifyNoInteractions(adapterRegistry);
    }

    @Test
    @DisplayName("USER 요청 - 다른 그룹 소속이면 ActuatorNotFoundException, Adapter로 안 나감")
    void control_USER_다른그룹_거절() {
        Location location = mock(Location.class);
        given(location.getLocationId()).willReturn(50L);
        Actuator actuator = Actuator.builder()
                .actuatorId(1L).actuatorType(ActuatorType.AIRCON)
                .currentState(state("power", "OFF"))
                .controlProvider(ControlProvider.SMART_THINGS).externalDeviceId("dev-001")
                .location(location)
                .build();
        given(actuatorRepository.findById(1L)).willReturn(Optional.of(actuator));
        given(locationRepository.findByLocationIdAndGroupGroupId(50L, 5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> facade.control(5L, 1L, Map.of("power", "ON"), ExecutedByType.USER, 1L))
                .isInstanceOf(ActuatorNotFoundException.class);

        verifyNoInteractions(adapterRegistry);
    }

    @Test
    @DisplayName("USER 요청 - 소유권 통과 시 정상 진행")
    void control_USER_소유권통과() {
        Location location = mock(Location.class);
        given(location.getLocationId()).willReturn(50L);
        Actuator actuator = Actuator.builder()
                .actuatorId(1L).actuatorType(ActuatorType.AIRCON)
                .currentState(state("power", "OFF"))
                .controlProvider(ControlProvider.SMART_THINGS).externalDeviceId("dev-001")
                .location(location)
                .build();
        given(actuatorRepository.findById(1L)).willReturn(Optional.of(actuator));
        given(locationRepository.findByLocationIdAndGroupGroupId(50L, 5L)).willReturn(Optional.of(location));
        given(adapterRegistry.get(ControlProvider.SMART_THINGS)).willReturn(adapter);
        given(adapter.control(any())).willReturn(new ActuatorControlResult(state("power", "ON"), null));

        facade.control(5L, 1L, Map.of("power", "ON"), ExecutedByType.USER, 1L);

        verify(actuatorService).updateActuatorState(5L, 1L, Map.of("power", "ON"), ExecutedByType.USER, 1L);
    }
}
