# 액추에이터 제어 흐름

---

## 1. 한눈에

기존에는 AI·룰엔진·사용자가 상태 변경을 요청하면 CORE가 `actuators.current_state` 컬럼만 갱신했습니다. 실제 기기로 명령이 나가는 경로가 없었습니다.

지금은 CORE가 공급자(SmartThings / LG ThinQ)별 **어댑터**를 통해 실제 API를 호출합니다. 개발·시연 환경에서는 그 API 자리에 **시뮬레이터**가 들어갑니다. 시뮬레이터는 두 공식 API의 경로·페이로드를 그대로 흉내 내므로, CORE 입장에서는 상대가 시뮬레이터인지 실제 클라우드인지 구별하지 않습니다.

- **CORE ↔ 시뮬레이터는 코드·DTO를 하나도 공유하지 않습니다.** HTTP + JSON 계약으로만 통신합니다.
- **시뮬레이터는 상태를 저장하지 않습니다.** "이 deviceId가 어느 공급자의 어떤 종류인가"라는 고정 카탈로그만 압니다.
- **상태의 진실은 CORE DB입니다.** 어댑터는 "공급자가 명령을 수락함 → 요청값이 적용됨"으로 간주하고 그 값을 CORE DB에 저장합니다.

---

## 2. 등장 컴포넌트

| 컴포넌트 | 위치 | 역할 |
|---|---|---|
| `SuggestionLogServiceImpl` / `ActuatorCommandExecutor` | InsightOn-ai | AI 제안 수락 시 CORE 내부 API 호출 |
| `ActuatorInternalController` | InsightOn-core `controller/internal/` | 룰엔진·AI 전용 진입점 |
| `UpdateActuatorStateByGroupUseCase` | `usecase/actuator/` | location + 종류로 대상 액추에이터 확정, 권한 검증 |
| `ActuatorControlFacade` | `usecase/actuator/` | **제어 흐름의 중심** — 병합·검증·어댑터 선택·성공 시 저장 |
| `ActuatorControlAdapterRegistry` | `domain/actuators/control/` | `ControlProvider` → 어댑터 매핑 |
| `SmartThingsActuatorAdapter` / `LgThinQActuatorAdapter` | `adapter/client/actuator/` | 공급자별 제어 구현 |
| `SmartThingsCommandAssembler` / `LgThinQControlAssembler` | 〃 | 중립 상태 → 공급자 JSON |
| `SmartThingsApiClient` / `LgThinQApiClient` | 〃 | `RestClient`로 실제 HTTP |
| `SmartThingsController` / `LgThinQController` | actuator-simulator | 공급자 공식 API 흉내 |
| `DeviceCatalogRegistry` | actuator-simulator `catalog/` | 14대 고정 카탈로그, id → (공급자, 종류) |

---

## 3. 전체 흐름

```mermaid
sequenceDiagram
    participant AI as InsightOn-ai
    participant IC as ActuatorInternalController
    participant UC as UpdateActuatorStateByGroupUseCase
    participant FC as ActuatorControlFacade
    participant RG as AdapterRegistry
    participant AD as SmartThingsActuatorAdapter
    participant AS as SmartThingsCommandAssembler
    participant CL as SmartThingsApiClient
    participant SIM as 시뮬레이터 SmartThingsController
    participant SVC as ActuatorService

    AI->>IC: PUT /internal/v1/groups/1/locations/3/actuators/state<br/>{actuatorType:AIRCON, command:temperature, commandValue:"25", callerService:AI_SYSTEM}
    IC->>UC: execute(groupId, locationId, request)
    UC->>UC: location 3 소유권 검증
    UC->>UC: location 3 의 AIRCON 액추에이터 조회 → actuatorId=9
    UC->>FC: control(1, 9, {temperature:25}, AI_SYSTEM, null)
    FC->>FC: DB 현재상태 + {temperature:25} 병합<br/>→ {power:ON, mode:COOL, temperature:25}
    FC->>FC: ActuatorCommandPreset.validateCommandValues (18~30 범위 등)
    FC->>RG: get(actuator.controlProvider = SMART_THINGS)
    RG-->>FC: SmartThingsActuatorAdapter
    FC->>AD: control(ActuatorControlCommand<br/>{externalDeviceId:"st-aircon-001", actuatorType:AIRCON, desiredState:{...}})
    AD->>AS: assemble(command)
    AS->>AS: switch(actuatorType) → AIRCON → airConditionerMode capability
    AS-->>AD: {"commands":[{switch:on}, {airConditionerMode:cool}, {thermostatCoolingSetpoint:25}]}
    AD->>CL: sendCommands("st-aircon-001", request)
    CL->>SIM: POST {base-url}/v1/devices/st-aircon-001/commands<br/>Authorization: Bearer <token>
    SIM->>SIM: 토큰 확인 → catalog.byId("st-aircon-001") → "SMART_THINGS / AIRCON"
    SIM-->>CL: {"results":[{"status":"ACCEPTED"} x3]}
    CL-->>AD: SmartThingsCommandResponse
    AD->>AD: results 전부 ACCEPTED 확인
    AD-->>FC: ActuatorControlResult(appliedState = desiredState)
    FC->>SVC: updateActuatorState(1, 9, appliedState, AI_SYSTEM, null)
    SVC->>SVC: entity.updateState() → core.actuators.current_state
    SVC->>SVC: recordRunLogs() → core.actuator_run_logs
```

---

## 4. AI — 제안 수락 → CORE 호출

| | 파일 |
|---|---|
| Feign 정의 | `InsightOn-ai` · `adapter/client/CoreClient.java` — `executeActuatorCommand` |
| 실행기 | `InsightOn-ai` · `adapter/client/ActuatorCommandExecutor.java` |
| 수락 처리 호출부 | `InsightOn-ai` · `domain/suggestion/service/impl/SuggestionLogServiceImpl.java:169` |

```
PUT /internal/v1/groups/{groupId}/locations/{locationId}/actuators/state
{
  "actuatorType":  "AIRCON",
  "command":       "temperature",      // CommandType.stateKey ("power" / "mode" / "temperature")
  "commandValue":  "25",               // 문자열
  "callerService": "AI_SYSTEM"         // USER 는 이 내부 API에서 거부됨
}
```

> ⚠️ **현재 AI 쪽 버그**: `CoreClient.executeActuatorCommand`의 경로가 `/internal/v1/locations/{id}/actuators/state`로 `/groups/{groupId}`가 빠져 있어, 지금 CORE와 맞지 않습니다. AI 팀에서 경로 + `groupId` 파라미터 추가가 필요합니다.

---

## 5. CORE 진입 — `ActuatorInternalController`

[`../src/main/java/com/insighton/core/controller/internal/ActuatorInternalController.java`](../src/main/java/com/insighton/core/controller/internal/ActuatorInternalController.java#L50)

```java
@PutMapping("/groups/{groupId}/locations/{locationId}/actuators/state")
public ResponseEntity<Void> updateActuatorStateByGroup(
        @PathVariable Long groupId,
        @PathVariable Long locationId,
        @Valid @RequestBody ActuatorCommandRequest request) {
    updateActuatorStateByGroupUseCase.execute(groupId, locationId, request);
    return ResponseEntity.ok().build();
}
```

`ActuatorCommandRequest` DTO: [`../src/main/java/com/insighton/core/domain/actuators/dto/ActuatorCommandRequest.java`](../src/main/java/com/insighton/core/domain/actuators/dto/ActuatorCommandRequest.java)

---

## 6. UseCase — 대상 확정 + 권한

[`../src/main/java/com/insighton/core/usecase/actuator/UpdateActuatorStateByGroupUseCase.java`](../src/main/java/com/insighton/core/usecase/actuator/UpdateActuatorStateByGroupUseCase.java#L27)

```java
public void execute(Long groupId, Long locationId, ActuatorCommandRequest request) {

    if (request.callerService() == ExecutedByType.USER)                 // 이 내부 API는 시스템만
        throw new InvalidServiceCredentialException("...USER가 호출할 수 없습니다");

    locationService.getLocationByGroupId(locationId, groupId);          // L35  location 소유권 (불일치 → 404)

    ActuatorType actuatorType = parseActuatorType(request.actuatorType());  // "AIRCON" → enum (잘못되면 400)

    List<Actuator> actuators = actuatorRepository
            .findByLocationLocationIdAndActuatorType(locationId, actuatorType);  // L41  이 location 의 AIRCON 전부
    if (actuators.isEmpty())
        throw new ActuatorLocationsActuatorTypeNotFound(locationId, actuatorType);   // 404

    Map<String, Object> partialState = Map.of(request.command(), request.commandValue());  // {"temperature":"25"}
    for (Actuator actuator : actuators) {
        actuatorControlFacade.control(                                  // L52  각 액추에이터마다
                groupId, actuator.getActuatorId(), partialState, request.callerService(), null);
    }
}
```

> AI는 개별 `actuatorId`를 모릅니다. `location + 종류`만 지정하면 여기서 대상을 `actuatorId=9`로 확정합니다.

---

## 7. Facade — `ActuatorControlFacade.control()`

[`../src/main/java/com/insighton/core/usecase/actuator/ActuatorControlFacade.java`](../src/main/java/com/insighton/core/usecase/actuator/ActuatorControlFacade.java#L33)

```java
public void control(Long groupsId, Long actuatorId, Map<String,Object> partialState,
                    ExecutedByType executedByType, Long actingUserId) {

    // ── 7-1. 빈 값 검사 ──────────────────────────────────────────────
    if (partialState == null || partialState.isEmpty())
        throw new InvalidActuatorValueException("...비어있음");          // → 400

    // ── 7-2. 엔티티 조회 ────────────────────────────────────────────
    Actuator actuator = actuatorRepository.findById(actuatorId)
            .orElseThrow(() -> new ActuatorNotFoundException(actuatorId));  // → 404

    // ── 7-3. 소유권 검증 (USER 요청만) ─────────────────────────────
    if (executedByType == ExecutedByType.USER) {                        // L45
        boolean belongsToGroup = locationRepository
                .findByLocationIdAndGroupGroupId(actuator.getLocation().getLocationId(), groupsId)
                .isPresent();
        if (!belongsToGroup) throw new ActuatorNotFoundException(actuatorId);  // 남의 그룹 → 404
    }

    // ── 7-4. UNBOUND 검사 ──────────────────────────────────────────
    if (actuator.getControlProvider() == null || actuator.getExternalDeviceId() == null)  // L54
        throw new InvalidActuatorValueException("제어 공급자가 연결되지 않은 액추에이터입니다 ...");  // → 400

    // ── 7-5. 상태 병합 ────────────────────────────────────────────
    Map<String,Object> mergedState = new HashMap<>(                     // L59
            actuator.getCurrentState() != null ? actuator.getCurrentState() : Map.of());
    mergedState.putAll(partialState);                                   // L61  {temperature:25} 덮어쓰기
    //  DB {power:ON, mode:COOL, temperature:24}  +  {temperature:25}
    //   = {power:ON, mode:COOL, temperature:25}

    // ── 7-6. 값 검증 ──────────────────────────────────────────────
    ActuatorCommandPreset.validateCommandValues(actuator.getActuatorType(), mergedState);  // L63

    // ── 7-7. 어댑터 선택 (공급자 판단) ─────────────────────────────
    ActuatorControlAdapter adapter = adapterRegistry.get(actuator.getControlProvider());   // L65

    // ── 7-8. 명령 객체 ────────────────────────────────────────────
    ActuatorControlCommand command = new ActuatorControlCommand(        // L70
            actuator.getExternalDeviceId(),   // "st-aircon-001"
            actuator.getActuatorType(),        // AIRCON
            mergedState);                      // {power:ON, mode:COOL, temperature:25}

    // ── 7-9. 어댑터 호출 (실제 HTTP는 여기서) ─────────────────────
    ActuatorControlResult result = adapter.control(command);            // L74

    // ── 7-10. 성공 후에만 저장 ────────────────────────────────────
    actuatorService.updateActuatorState(                                // L77
            groupsId, actuatorId, result.appliedState(), executedByType, actingUserId);
}
```

### 7-5 왜 병합하나

공급자에게 "온도만 바꿔"가 아니라 **"이 기기의 최종 상태 전체"**를 보냅니다. 온도만 눌러도 전원·모드가 DB에서 함께 실려 나갑니다.

### 7-6 값 검증 규칙

[`../src/main/java/com/insighton/core/domain/actuators/policy/ActuatorCommandPreset.java`](../src/main/java/com/insighton/core/domain/actuators/policy/ActuatorCommandPreset.java#L16)

```java
AIRCON        → POWER_STATUS {ON,OFF} · OPERATION_MODE {COOL,DRY,FAN,AUTO} · SET_TEMPERATURE [18,30]
AIR_PURIFIER  → POWER_STATUS {ON,OFF} · OPERATION_MODE {AUTO,SLEEP,TURBO}
VENTILATION_FAN → POWER_STATUS {ON,OFF} · OPERATION_MODE {LOW,MID,HIGH}
```

`validateCommandValues`는 `mergedState`의 각 키를 `CommandType.fromStateKey`로 매핑 (`power`→POWER_STATUS 등)한 뒤 위 규칙으로 검증. 하나라도 어기면 `InvalidActuatorValueException` → 400.

---

## 8. 두 번의 "제품 판단"

`adapter.control(command)` **이 줄에서는 아무것도 판단하지 않습니다.** 판단은 그 전에 두 번 끝나 있습니다.

### 판단 ① — 공급자 (SmartThings냐 LG냐)

[`ActuatorControlFacade.java:65`](../src/main/java/com/insighton/core/usecase/actuator/ActuatorControlFacade.java#L65)
```java
ActuatorControlAdapter adapter = adapterRegistry.get(actuator.getControlProvider());
```

[`ActuatorControlAdapterRegistry.java`](../src/main/java/com/insighton/core/domain/actuators/control/ActuatorControlAdapterRegistry.java)
```java
public ActuatorControlAdapterRegistry(List<ActuatorControlAdapter> adapterList) {
    this.adapters = adapterList.stream()
            .collect(Collectors.toMap(ActuatorControlAdapter::supports, Function.identity()));
    // {SMART_THINGS: SmartThingsActuatorAdapter, LG_THINQ: LgThinQActuatorAdapter}
}
public ActuatorControlAdapter get(ControlProvider provider) {
    ActuatorControlAdapter adapter = adapters.get(provider);
    if (adapter == null) throw new UnsupportedControlProviderException(provider);  // → 400
    return adapter;
}
```

- 근거: `Actuator` 엔티티의 **`control_provider` 컬럼** ([`Actuator.java:50`](../src/main/java/com/insighton/core/domain/actuators/entity/Actuator.java#L50))
- `SMART_THINGS` → `SmartThingsActuatorAdapter` 인스턴스 반환
- 이후 `adapter.control(command)`는 **Java 다형성**으로 `SmartThingsActuatorAdapter.control()`이 실행됨. `if (provider == ...)` 분기는 어디에도 없음 — 어느 클래스의 메서드가 실행될지는 65번 줄에서 결정됨.

### 판단 ② — 종류 (에어컨 / 공기청정기 / 환풍기)

`command`에 종류가 실려 있음 — [`ActuatorControlCommand.java`](../src/main/java/com/insighton/core/domain/actuators/control/ActuatorControlCommand.java)
```java
public record ActuatorControlCommand(
        String externalDeviceId,    // "st-aircon-001"   ← Actuator.external_device_id
        ActuatorType actuatorType,  // AIRCON            ← Actuator.actuator_type
        Map<String,Object> desiredState) {}
```

[`SmartThingsCommandAssembler.java`](../src/main/java/com/insighton/core/adapter/client/actuator/smartthings/SmartThingsCommandAssembler.java#L63)
```java
private SmartThingsCommandRequest.Command modeCommand(ActuatorType actuatorType, Object mode) {
    return switch (actuatorType) {                                     // ← 종류 판단
        case AIRCON          -> capabilityCommand("airConditionerMode",  "setAirConditionerMode", ...);
        case AIR_PURIFIER    -> capabilityCommand("airPurifierFanMode",   "setAirPurifierFanMode", ...);
        case VENTILATION_FAN -> capabilityCommand("fanSpeed",             "setFanSpeed", ...);  // 인자 정수
    };
}
```

- 근거: `Actuator` 엔티티의 **`actuator_type` 컬럼** ([`Actuator.java:36`](../src/main/java/com/insighton/core/domain/actuators/entity/Actuator.java#L36))

### 판단 근거 정리

| 무엇 | 코드 | DB 컬럼 (`core.actuators`) |
|---|---|---|
| 공급자 → 어느 **어댑터 클래스** | `ActuatorControlFacade:65` `adapterRegistry.get(getControlProvider())` | `control_provider` |
| 종류 → 어느 **capability / 속성그룹** | `SmartThingsCommandAssembler:63` `switch(actuatorType)` | `actuator_type` |
| 어느 **기기**로 (URL 경로) | `SmartThingsApiClient:38` `.uri(".../{deviceId}/commands", externalDeviceId)` | `external_device_id` |

이 세 컬럼은 **Front "공급자 장치" 섹션에서 액추에이터를 등록할 때** 채워집니다. 공급자를 고르면 `control_provider`, 목록에서 장치를 고르면 `external_device_id`, 그 장치 타입에 맞춰 `actuator_type`이 자동 지정됩니다.

---

## 9. 어댑터 → JSON 변환 → 전송

### `SmartThingsActuatorAdapter.control()`

[`../src/main/java/com/insighton/core/adapter/client/actuator/smartthings/SmartThingsActuatorAdapter.java`](../src/main/java/com/insighton/core/adapter/client/actuator/smartthings/SmartThingsActuatorAdapter.java#L36)
```java
public ActuatorControlResult control(ActuatorControlCommand command) {
    SmartThingsCommandRequest request = assembler.assemble(command);              // 9-1
    log.info("[SmartThings] {} → {}", command.externalDeviceId(), toJson(request));
    SmartThingsCommandResponse response = apiClient.sendCommands(command.externalDeviceId(), request);  // 9-2

    if (response == null || response.results() == null || response.results().isEmpty())
        throw new SmartThingsApiException("SmartThings 응답에 results가 없습니다");
    boolean allAccepted = response.results().stream()
            .allMatch(r -> "ACCEPTED".equalsIgnoreCase(r.status()));
    if (!allAccepted)
        throw new SmartThingsApiException("SmartThings가 일부 명령을 수락하지 않았습니다: " + response.results());

    // 공급자가 명령을 수락했으므로, 요청한 desiredState 가 그대로 적용됐다고 보고 CORE 에 반영한다
    return new ActuatorControlResult(command.desiredState(), summarize(response));   // 9-3
}
```

### 9-1. Assembler — 중립 상태 → SmartThings JSON

[`SmartThingsCommandAssembler.java:32`](../src/main/java/com/insighton/core/adapter/client/actuator/smartthings/SmartThingsCommandAssembler.java#L32)
```java
public SmartThingsCommandRequest assemble(ActuatorControlCommand command) {
    Map<String,Object> state = command.desiredState();   // {power:ON, mode:COOL, temperature:25}
    List<Command> commands = new ArrayList<>();

    if (state.containsKey("power"))       commands.add(switchCommand(state.get("power")));
    if (state.containsKey("mode"))        commands.add(modeCommand(command.actuatorType(), state.get("mode")));
    if (state.containsKey("temperature")){ requireAircon(command.actuatorType(), "temperature");   // AIRCON만
                                           commands.add(coolingSetpointCommand(state.get("temperature"))); }
    ...
}
```

결과:
```json
{"commands":[
  {"component":"main","capability":"switch","command":"on","arguments":[]},
  {"component":"main","capability":"airConditionerMode","command":"setAirConditionerMode","arguments":["cool"]},
  {"component":"main","capability":"thermostatCoolingSetpoint","command":"setCoolingSetpoint","arguments":[25]}
]}
```

LG면 [`LgThinQControlAssembler.java`](../src/main/java/com/insighton/core/adapter/client/actuator/lg/LgThinQControlAssembler.java)가 대신:
```json
{"operation":{"airConOperationMode":"POWER_ON"},
 "airConJobMode":{"currentJobMode":"COOL"},
 "temperature":{"targetTemperature":25}}
```

### 9-2. ApiClient — 실제 HTTP

[`SmartThingsApiClient.java:35`](../src/main/java/com/insighton/core/adapter/client/actuator/smartthings/SmartThingsApiClient.java#L35)
```java
return smartThingsRestClient.post()
        .uri("/v1/devices/{deviceId}/commands", deviceId)   // deviceId = "st-aircon-001"
        .body(request)
        .retrieve()
        .body(SmartThingsCommandResponse.class);
```

`smartThingsRestClient` 빈: [`ActuatorRestClientConfig.java`](../src/main/java/com/insighton/core/common/config/ActuatorRestClientConfig.java#L26)
```java
RestClient.builder()
        .baseUrl(smartThingsBaseUrl)                                  // actuator.smartthings.base-url
        .defaultHeader("Authorization", "Bearer " + smartThingsToken) // actuator.smartthings.token
        .build();
```

`base-url` 값 ([`application-local.properties`](../src/main/resources/application-local.properties)):
```
actuator.smartthings.base-url = http://localhost:8090/smartthings   # 로컬 = 시뮬레이터
actuator.lg-thinq.base-url    = http://localhost:8090/lg
# 실제: https://api.smartthings.com  등으로만 바꾸면 됨
```

최종 요청: `POST http://localhost:8090/smartthings/v1/devices/st-aircon-001/commands`

---

## 10. 시뮬레이터 — 받아서 응답

`actuator-simulator` · `src/main/java/com/insighton/actuatorsimulator/smartthings/SmartThingsController.java` → `executeCommands()`
```java
tokenValidator.validate(authorization);          // 1. Bearer 토큰 (틀리면 401)
catalog.byId(deviceId);                          // 2. catalog.byId("st-aircon-001")  (없으면 404)
log.info("[SMART_THINGS] {} ← {}", deviceId, toJson(request));   // 받은 raw JSON 로그
ProviderCommand command = translator.translate(deviceId, request);   // 3. JSON → 중립 명령 (역변환)
return assembler.commandResponse(command.desiredState().keySet());   // 4. {"results":[ACCEPTED...]}
```

- **카탈로그** (`catalog/DeviceCatalogRegistry.java`) — 14대 고정 상수. `byId("st-aircon-001")` → `DeviceCatalog("st-aircon-001", "SMART_THINGS", "AIRCON", "회의실 에어컨")`
- **RequestTranslator** (`smartthings/SmartThingsRequestTranslator.java`) — 9-1 Assembler의 역방향. `capability:"switch"` → `power`, `"cool"` → `COOL` 등. 지원 안 하는 capability면 400.
- **상태를 저장하지 않습니다.** `translate`를 호출하는 이유는 (a) 잘못된 payload면 400을 내기 위해, (b) 응답 result 개수를 맞추기 위해서입니다.

응답: `{"results":[{"id":"<uuid>","status":"ACCEPTED"}, {...}, {...}]}` (키 3개니까 3개)

---

## 11. 다시 CORE — 수락 확인 후 저장

### 11-1. 어댑터 (9-3)

`response.results()` 3개 전부 `"ACCEPTED"` → `allAccepted == true`
```java
return new ActuatorControlResult(command.desiredState(), summarize(response));
//                               ↑ mergedState = {power:ON, mode:COOL, temperature:25}
```

**시뮬레이터가 응답에 담은 내용은 쓰지 않습니다.** "수락했으니 내가 보낸 게 적용됐다." (실제 SmartThings도 command 응답엔 상태를 안 줍니다.)

### 11-2. Facade → ActuatorService

[`../src/main/java/com/insighton/core/domain/actuators/service/impl/ActuatorServiceImpl.java`](../src/main/java/com/insighton/core/domain/actuators/service/impl/ActuatorServiceImpl.java#L104)
```java
public void updateActuatorState(Long groupsId, Long actuatorId, Map<String,Object> newState,
                                ExecutedByType executedByType, Long actingUserId) {
    ...
    Actuator entity = actuatorRepository.findById(actuatorId).orElseThrow(...);
    if (isUserRequest) validateActuatorOwnership(entity, groupsId);       // USER면 소유권 한 번 더

    entity.updateState(newState);                                         // L121  core.actuators.current_state
    actuatorRunLogService.recordRunLogs(entity, newState, executedByType, // L124  core.actuator_run_logs
            isUserRequest ? actingUserId : null);
}
```

- `entity.updateState()` → JPA dirty checking → 트랜잭션 커밋 시 `UPDATE core.actuators SET current_state = ...`
- `recordRunLogs` → `INSERT core.actuator_run_logs`: 액추에이터 · 바뀐 값 · `AI_SYSTEM` · 시각

---

## 12. 사용자가 Front에서 직접 조작하는 경로

§4~6이 아래로 바뀌고, §7 이후는 동일합니다.

| | 파일 |
|---|---|
| Front JS | `InsightOn-front` · `static/js/actuator-panel.js` — 스텝퍼/토글/칩 → `PUT /locations/{id}/actuators/{id}/state` |
| Front 프록시 | `InsightOn-front` · `controller/core/ActuatorController.java` `updateState()` — MANAGER 검증 → Feign |
| CORE 진입 | [`../src/main/java/com/insighton/core/controller/api/ActuatorController.java`](../src/main/java/com/insighton/core/controller/api/ActuatorController.java#L89) `updateActuatorState()` |
| UseCase | [`../src/main/java/com/insighton/core/usecase/actuator/UpdateActuatorStateUseCase.java`](../src/main/java/com/insighton/core/usecase/actuator/UpdateActuatorStateUseCase.java) — 매니저 권한 검증 → `Facade.control(..., ExecutedByType.USER, userId)` |

차이:
- `callerService` 자리에 `ExecutedByType.USER` → §7-3 소유권 검증이 **켜짐**
- 개별 `actuatorId`를 Front가 알고 있어 §6의 "location+종류로 조회" 단계 없음

---

## 13. 실패 시

| 실패 지점 | 예외 | HTTP |
|---|---|---|
| 부분 상태 비어 있음 (§7-1) | `InvalidActuatorValueException` | 400 |
| 액추에이터 없음 / 남의 그룹 (§7-2, §7-3) | `ActuatorNotFoundException` | 404 |
| UNBOUND — 공급자 미연결 (§7-4) | `InvalidActuatorValueException` | 400 |
| 허용 안 되는 값 (§7-6) | `InvalidActuatorValueException` | 400 |
| 미지원 공급자 (§7-7) | `UnsupportedControlProviderException` | 400 |
| 공급자 API 4xx/5xx · 연결 실패 (§9-2) | `SmartThingsApiException` / `LgThinQApiException` (= `ActuatorControlException`) | **502** |

502 매핑: [`../src/main/java/com/insighton/core/common/exception/GlobalExceptionHandler.java`](../src/main/java/com/insighton/core/common/exception/GlobalExceptionHandler.java)

> **어댑터 호출(§9)이 실패하면 §11(저장)은 실행되지 않습니다.** "공급자는 실패했는데 CORE만 ON으로 보이는" 불일치를 막는 게 이 순서(외부 호출 → 성공 시에만 저장)의 이유입니다. 그래서 `Facade.control()`에는 `@Transactional`이 없습니다.

---

## 14. DB 변경

`ddl-auto=validate`라 코드 배포 전에 수동으로:
```sql
ALTER TABLE core.actuators ADD COLUMN control_provider    varchar(30);
ALTER TABLE core.actuators ADD COLUMN external_device_id  varchar(150);
```
둘 다 nullable. 값이 없으면 **UNBOUND** — §7-4에서 400.

---

## 15. 새 공급자를 추가하려면

건드리는 곳은 어댑터 계층뿐입니다.

1. `ControlProvider` enum에 값 추가
2. `ActuatorControlAdapter`를 구현한 `XxxActuatorAdapter` (`@Component`) — Registry가 자동 인식
3. 그 공급자 형식의 `Assembler` · `ApiClient` · `dto/`
4. `ActuatorRestClientConfig`에 `RestClient` 빈 + properties에 base-url/token
5. Front 등록 폼과 "공급자 장치" 섹션의 공급자 목록에 추가
6. (시연용) 시뮬레이터에 `xxx/` 패키지 + 카탈로그 항목

`ActuatorControlFacade` · UseCase · 컨트롤러 · DB 스키마는 그대로입니다.
