package com.insighton.core.actuator.control;

import com.insighton.core.domain.actuators.control.ActuatorControlAdapter;
import com.insighton.core.domain.actuators.control.ActuatorControlAdapterRegistry;
import com.insighton.core.domain.actuators.control.ControlProvider;
import com.insighton.core.domain.actuators.exception.UnsupportedControlProviderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ActuatorControlAdapterRegistryTest {

    private static ActuatorControlAdapter adapterFor(ControlProvider provider) {
        ActuatorControlAdapter adapter = mock(ActuatorControlAdapter.class);
        given(adapter.supports()).willReturn(provider);
        return adapter;
    }

    @Test
    @DisplayName("등록된 provider면 supports() 기준으로 매핑된 Adapter 반환")
    void get_등록된provider() {
        ActuatorControlAdapter st = adapterFor(ControlProvider.SMART_THINGS);
        ActuatorControlAdapter lg = adapterFor(ControlProvider.LG_THINQ);
        ActuatorControlAdapterRegistry registry = new ActuatorControlAdapterRegistry(List.of(st, lg));

        assertThat(registry.get(ControlProvider.SMART_THINGS)).isSameAs(st);
        assertThat(registry.get(ControlProvider.LG_THINQ)).isSameAs(lg);
    }

    @Test
    @DisplayName("구현체가 없는 provider면 UnsupportedControlProviderException")
    void get_미등록provider() {
        ActuatorControlAdapterRegistry registry = new ActuatorControlAdapterRegistry(
                List.of(adapterFor(ControlProvider.SMART_THINGS)));

        assertThatThrownBy(() -> registry.get(ControlProvider.LG_THINQ))
                .isInstanceOf(UnsupportedControlProviderException.class);
    }

    @Test
    @DisplayName("Adapter가 하나도 없으면 어떤 provider를 조회해도 UnsupportedControlProviderException")
    void get_빈레지스트리() {
        ActuatorControlAdapterRegistry registry = new ActuatorControlAdapterRegistry(List.of());

        assertThatThrownBy(() -> registry.get(ControlProvider.SMART_THINGS))
                .isInstanceOf(UnsupportedControlProviderException.class);
    }
}
