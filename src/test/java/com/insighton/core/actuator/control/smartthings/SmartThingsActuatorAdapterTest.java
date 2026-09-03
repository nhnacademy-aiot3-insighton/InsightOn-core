package com.insighton.core.actuator.control.smartthings;

import com.insighton.core.adapter.client.actuator.smartthings.SmartThingsActuatorAdapter;
import com.insighton.core.adapter.client.actuator.smartthings.SmartThingsApiClient;
import com.insighton.core.adapter.client.actuator.smartthings.SmartThingsApiException;
import com.insighton.core.adapter.client.actuator.smartthings.SmartThingsCommandAssembler;
import com.insighton.core.adapter.client.actuator.smartthings.dto.SmartThingsCommandRequest;
import com.insighton.core.adapter.client.actuator.smartthings.dto.SmartThingsCommandResponse;
import com.insighton.core.domain.actuators.control.ActuatorControlCommand;
import com.insighton.core.domain.actuators.control.ActuatorControlResult;
import com.insighton.core.domain.actuators.control.ControlProvider;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmartThingsActuatorAdapterTest {

    @Mock private SmartThingsCommandAssembler assembler;
    @Mock private SmartThingsApiClient apiClient;

    @InjectMocks
    private SmartThingsActuatorAdapter adapter;

    private static ActuatorControlCommand command() {
        return new ActuatorControlCommand("st-aircon-001", ActuatorType.AIRCON, Map.of("power", "ON"));
    }

    private static SmartThingsCommandRequest assembledRequest() {
        return new SmartThingsCommandRequest(List.of(
                new SmartThingsCommandRequest.Command("main", "switch", "on", List.of())));
    }

    @Test
    @DisplayName("supports()는 SMART_THINGS")
    void supports() {
        assertThat(adapter.supports()).isEqualTo(ControlProvider.SMART_THINGS);
    }

    @Test
    @DisplayName("모든 result가 ACCEPTED면 desiredState를 그대로 담은 ActuatorControlResult 반환")
    void control_성공() {
        ActuatorControlCommand cmd = command();
        given(assembler.assemble(cmd)).willReturn(assembledRequest());
        given(apiClient.sendCommands(eq("st-aircon-001"), any()))
                .willReturn(new SmartThingsCommandResponse(List.of(
                        new SmartThingsCommandResponse.Result("id-1", "ACCEPTED"))));

        ActuatorControlResult result = adapter.control(cmd);

        assertThat(result.appliedState()).isEqualTo(cmd.desiredState());
        verify(apiClient).sendCommands(eq("st-aircon-001"), any());
    }

    @Test
    @DisplayName("results가 비어 있으면 SmartThingsApiException")
    void control_빈results() {
        given(assembler.assemble(any())).willReturn(assembledRequest());
        given(apiClient.sendCommands(any(), any()))
                .willReturn(new SmartThingsCommandResponse(List.of()));

        assertThatThrownBy(() -> adapter.control(command()))
                .isInstanceOf(SmartThingsApiException.class);
    }

    @Test
    @DisplayName("일부 result가 ACCEPTED가 아니면 SmartThingsApiException")
    void control_일부거부() {
        given(assembler.assemble(any())).willReturn(assembledRequest());
        given(apiClient.sendCommands(any(), any()))
                .willReturn(new SmartThingsCommandResponse(List.of(
                        new SmartThingsCommandResponse.Result("id-1", "ACCEPTED"),
                        new SmartThingsCommandResponse.Result("id-2", "FAILED"))));

        assertThatThrownBy(() -> adapter.control(command()))
                .isInstanceOf(SmartThingsApiException.class);
    }

    @Test
    @DisplayName("apiClient가 던진 SmartThingsApiException은 그대로 전파")
    void control_클라이언트예외전파() {
        given(assembler.assemble(any())).willReturn(assembledRequest());
        given(apiClient.sendCommands(any(), any()))
                .willThrow(new SmartThingsApiException("공급자 500"));

        assertThatThrownBy(() -> adapter.control(command()))
                .isInstanceOf(SmartThingsApiException.class)
                .hasMessageContaining("공급자 500");
    }

    @Test
    @DisplayName("supportedValues - SmartThings 에어컨 mode엔 AIRCLEAN 없음, windDirection은 있음")
    void supportedValues() {
        java.util.Map<String, java.util.List<String>> ac = adapter.supportedValues(ActuatorType.AIRCON);
        assertThat(ac.get("mode")).containsExactly("COOL", "DRY", "FAN", "AUTO");
        assertThat(ac.get("mode")).doesNotContain("AIRCLEAN");
        assertThat(ac.get("windDirection")).containsExactly("FIXED", "SWING");
    }
}
