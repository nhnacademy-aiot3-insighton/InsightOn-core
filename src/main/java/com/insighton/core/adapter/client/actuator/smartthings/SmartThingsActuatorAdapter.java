package com.insighton.core.adapter.client.actuator.smartthings;

import com.insighton.core.adapter.client.actuator.smartthings.dto.SmartThingsCommandRequest;
import com.insighton.core.adapter.client.actuator.smartthings.dto.SmartThingsCommandResponse;
import com.insighton.core.adapter.client.actuator.smartthings.dto.SmartThingsDeviceListResponse;
import com.insighton.core.domain.actuators.control.ActuatorControlAdapter;
import com.insighton.core.domain.actuators.control.ActuatorControlCommand;
import com.insighton.core.domain.actuators.control.ActuatorControlResult;
import com.insighton.core.domain.actuators.control.ControlProvider;
import com.insighton.core.domain.actuators.control.ProviderDevice;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

// SMART_THINGS 공급자용 Adapter. Registry가 이 빈을 supports() 기준으로 자동 편입한다.
@Component
@RequiredArgsConstructor
@Slf4j
public class SmartThingsActuatorAdapter implements ActuatorControlAdapter {

    private static final String ACCEPTED = "ACCEPTED";

    private final SmartThingsCommandAssembler assembler;
    private final SmartThingsApiClient apiClient;
    private final ObjectMapper objectMapper;

    @Override
    public ControlProvider supports() {
        return ControlProvider.SMART_THINGS;
    }

    @Override
    public ActuatorControlResult control(ActuatorControlCommand command) {
        SmartThingsCommandRequest request = assembler.assemble(command);
        log.info("[SmartThings] {} → {}", command.externalDeviceId(), toJson(request));
        SmartThingsCommandResponse response = apiClient.sendCommands(command.externalDeviceId(), request);

        if (response == null || response.results() == null || response.results().isEmpty()) {
            throw new SmartThingsApiException("SmartThings 응답에 results가 없습니다");
        }
        boolean allAccepted = response.results().stream()
                .allMatch(r -> r.status() != null && ACCEPTED.equalsIgnoreCase(r.status()));
        if (!allAccepted) {
            throw new SmartThingsApiException("SmartThings가 일부 명령을 수락하지 않았습니다: " + response.results());
        }

        // 공급자가 명령을 수락했으므로, 요청한 desiredState가 그대로 적용됐다고 보고 CORE에 반영한다.
        return new ActuatorControlResult(command.desiredState(), summarize(response));
    }

    @Override
    public List<ProviderDevice> listDevices() {
        SmartThingsDeviceListResponse response = apiClient.listDevices();
        if (response == null || response.items() == null) {
            return List.of();
        }
        return response.items().stream()
                .map(item -> new ProviderDevice(item.deviceId(), item.label(), item.type()))
                .toList();
    }

    private String summarize(SmartThingsCommandResponse response) {
        return "results=" + response.results().size() + " all " + ACCEPTED;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
