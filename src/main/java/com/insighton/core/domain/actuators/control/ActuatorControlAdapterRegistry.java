package com.insighton.core.domain.actuators.control;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.insighton.core.domain.actuators.exception.UnsupportedControlProviderException;
import org.springframework.stereotype.Component;

// provider 값으로 알맞은 Adapter를 찾아줌 - 신규 공급자는 ActuatorControlAdapter 구현체 추가만 하면
// 여기 자동으로 편입됨 (기존 서비스 조건문 수정 불필요)
@Component
public class ActuatorControlAdapterRegistry {

    private final Map<ControlProvider, ActuatorControlAdapter> adapters;

    public ActuatorControlAdapterRegistry(List<ActuatorControlAdapter> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(ActuatorControlAdapter::supports, Function.identity()));
    }

    public ActuatorControlAdapter get(ControlProvider provider) {
        ActuatorControlAdapter adapter = adapters.get(provider);
        if (adapter == null) {
            throw new UnsupportedControlProviderException(provider);
        }
        return adapter;
    }
}
