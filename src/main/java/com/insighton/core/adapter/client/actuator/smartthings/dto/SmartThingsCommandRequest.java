package com.insighton.core.adapter.client.actuator.smartthings.dto;

import java.util.List;

public record SmartThingsCommandRequest(List<Command> commands) {

    public record Command(
            String component,
            String capability,
            String command,
            List<Object> arguments
    ) {
    }
}
