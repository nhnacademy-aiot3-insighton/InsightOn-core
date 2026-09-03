package com.insighton.core.adapter.client.actuator.smartthings.dto;

import java.util.List;

// SmartThings 공식 "Execute commands" 요청 형식.
// POST /v1/devices/{deviceId}/commands  body:
// { "commands": [ { "component": "main", "capability": "switch", "command": "on", "arguments": [] } ] }
public record SmartThingsCommandRequest(List<Command> commands) {

    public record Command(
            String component,
            String capability,
            String command,
            List<Object> arguments
    ) {
    }
}
