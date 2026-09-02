package com.insighton.core.actuator.control.lg;

import com.insighton.core.adapter.client.actuator.lg.LgThinQActuatorAdapter;
import com.insighton.core.adapter.client.actuator.lg.LgThinQApiClient;
import com.insighton.core.adapter.client.actuator.lg.LgThinQApiException;
import com.insighton.core.adapter.client.actuator.lg.LgThinQControlAssembler;
import com.insighton.core.adapter.client.actuator.lg.dto.LgThinQControlRequest;
import com.insighton.core.adapter.client.actuator.lg.dto.LgThinQControlResponse;
import com.insighton.core.adapter.client.actuator.lg.dto.LgThinQDeviceListItem;
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
class LgThinQActuatorAdapterTest {

    @Mock private LgThinQControlAssembler assembler;
    @Mock private LgThinQApiClient apiClient;

    @InjectMocks
    private LgThinQActuatorAdapter adapter;

    private static ActuatorControlCommand command() {
        return new ActuatorControlCommand("lg-aircon-001", ActuatorType.AIRCON, Map.of("power", "ON"));
    }

    private static LgThinQControlRequest assembled() {
        return new LgThinQControlRequest(new LgThinQControlRequest.Operation("POWER_ON"), null, null, null, null);
    }

    @Test
    @DisplayName("supports()는 LG_THINQ")
    void supports() {
        assertThat(adapter.supports()).isEqualTo(ControlProvider.LG_THINQ);
    }

    @Test
    @DisplayName("error 없는 응답이면 desiredState를 담은 ActuatorControlResult 반환")
    void control_성공() {
        ActuatorControlCommand cmd = command();
        given(assembler.assemble(cmd)).willReturn(assembled());
        given(apiClient.control(eq("lg-aircon-001"), any()))
                .willReturn(new LgThinQControlResponse("m-1", null));

        ActuatorControlResult result = adapter.control(cmd);

        assertThat(result.appliedState()).isEqualTo(cmd.desiredState());
        verify(apiClient).control(eq("lg-aircon-001"), any());
    }

    @Test
    @DisplayName("응답에 error가 있으면 LgThinQApiException")
    void control_error응답() {
        given(assembler.assemble(any())).willReturn(assembled());
        given(apiClient.control(any(), any()))
                .willReturn(new LgThinQControlResponse(null,
                        new LgThinQControlResponse.Error("2000", "unsupported property")));

        assertThatThrownBy(() -> adapter.control(command()))
                .isInstanceOf(LgThinQApiException.class);
    }

    @Test
    @DisplayName("apiClient가 던진 LgThinQApiException은 그대로 전파")
    void control_클라이언트예외전파() {
        given(assembler.assemble(any())).willReturn(assembled());
        given(apiClient.control(any(), any())).willThrow(new LgThinQApiException("공급자 500"));

        assertThatThrownBy(() -> adapter.control(command()))
                .isInstanceOf(LgThinQApiException.class)
                .hasMessageContaining("공급자 500");
    }

    @Test
    @DisplayName("listDevices - apiClient 응답을 ProviderDevice로 변환")
    void listDevices() {
        given(apiClient.listDevices()).willReturn(List.of(
                new LgThinQDeviceListItem("lg-aircon-001",
                        new LgThinQDeviceListItem.DeviceInfo("AIRCON", "대회의실 에어컨"))));

        var result = adapter.listDevices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).externalDeviceId()).isEqualTo("lg-aircon-001");
        assertThat(result.get(0).name()).isEqualTo("대회의실 에어컨");
        assertThat(result.get(0).actuatorType()).isEqualTo("AIRCON");
    }
}
