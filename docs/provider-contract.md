# 공급자 계약 · 경로분기 스펙

CORE 중립 명령 하나가 공급자(SmartThings / LG ThinQ) × 제품종류(에어컨 / 공기청정기 / 환풍기)에 따라
**어느 URL로, 어떤 헤더로, 어떤 JSON으로** 나가는지 전부 정의. 시뮬레이터는 이 계약의 로컬 대역이며,
실연동 = `base-url` + 토큰/헤더 값 교체(코드 0).

> `[근사]` 표시 = 실제 공급자 문서에서 확정 못 한 값. 코드에 `// TODO: 실제 profile로 확정` 주석. 나머지는 실제 API 문서/SDK 기준.

---

## 0. 분기 1단계 — 공급자 → 어댑터

`Actuator.control_provider` (DB 컬럼) → `ActuatorControlAdapterRegistry.get(provider)` — 분기문 없음, `Map<ControlProvider, Adapter>` 조회.

| `control_provider` | 어댑터 |
|---|---|
| `SMART_THINGS` | `SmartThingsActuatorAdapter` |
| `LG_THINQ` | `LgThinQActuatorAdapter` |

---

## 1. URL — base-url(config) + path(client 코드)

`path`는 client의 `.uri(...)`에 고정. `base-url`만 프로파일별로 바뀐다.

| 공급자 | base-url (실제) | base-url (로컬=시뮬) | 제어 | 목록 | 프로파일 / 상태 |
|---|---|---|---|---|---|
| **SmartThings** | `https://api.smartthings.com` | `http://localhost:8090/smartthings` | `POST /v1/devices/{id}/commands` | `GET /v1/devices` | `GET /v1/devices/{id}/status` |
| **LG ThinQ** | `https://api-kic.lgthinq.com` (KR)<br>`-aic` (US) / `-eic` (EU) | `http://localhost:8090/lg` | `POST /devices/{id}/control` | `GET /devices` | `GET /devices/{id}/profile`<br>`GET /devices/{id}/state` |

```properties
# application-{profile}.properties
# 실제: https://api.smartthings.com          로컬: 시뮬레이터
actuator.smartthings.base-url=http://localhost:8090/smartthings
# 실제: https://api-kic.lgthinq.com (KR)      로컬: 시뮬레이터
actuator.lg-thinq.base-url=http://localhost:8090/lg
```

---

## 2. 헤더 — 공급자별

| 헤더 | SmartThings | LG ThinQ |
|---|---|---|
| `Authorization: Bearer <token>` | ✅ | ✅ |
| `Content-Type: application/json` | ✅ | ✅ |
| `x-api-key` | — | ✅ 공개 고정키 `v6GFvkweNo7DK7yD3ylIZ9w52aKBU0eJ7wLXkSR3` (SDK 내장) |
| `x-client-id` | — | ✅ 우리 앱 UUID4 (config, 고정) |
| `x-message-id` | — | ✅ 요청마다 `base64url(uuid).substring(0,22)` |
| `x-country` | — | ✅ `KR` |
| `x-service-phase` | — | ✅ `OP` |
| `x-conditional-control: true` | — | ✅ 제어 요청만 (상태 확인 후 제어 가능할 때만 실행) |

- SmartThings: `ActuatorRestClientConfig.smartThingsRestClient()` 의 `defaultHeader` 로 충분.
- LG: `x-api-key`·`x-client-id`·`x-country`·`x-service-phase` 는 `defaultHeader`, `x-message-id`·`x-conditional-control` 는 `LgThinQApiClient` 에서 요청마다.

```properties
# 실제: LG ThinQ Developer 사이트에서 발급받은 PAT / 우리 앱 client-id
actuator.lg-thinq.token=local-sim-token
actuator.lg-thinq.api-key=v6GFvkweNo7DK7yD3ylIZ9w52aKBU0eJ7wLXkSR3
actuator.lg-thinq.client-id=00000000-0000-4000-8000-000000000001
actuator.lg-thinq.country=KR
```

---

## 3. 중립 어휘 (CORE·Front 공용)

| 명령 키 | 값 | 종류 | AI/룰엔진 |
|---|---|---|---|
| `power` | `ON` / `OFF` | 전 종류 | ✅ |
| `mode` | 에어컨: `COOL` `DRY` `FAN` `AUTO` (+ LG 전용 `AIRCLEAN`)<br>공기청정기: `AUTO` `SLEEP` `TURBO`<br>환풍기: `LOW` `MID` `HIGH` | 종류별 | ✅ (공통값만) |
| `windDirection` | `FIXED` / `SWING` | 에어컨만 | ❌ 수동 조작 전용 |
| `temperature` | 정수 18~30 | 에어컨만 | ✅ |

- `windDirection`·`AIRCLEAN` = **액추에이터 패널(수동 조작) 전용.** AI·룰엔진은 자기 검증기가 공통값만 통과시키므로 이 값들을 못 씀 (변경 불필요).
- 검증: `ActuatorCommandPreset` (종류별, `mode`는 공급자 union). `NeutralCommand` enum이 stateKey를,
  `SmartThingsVocab`/`LgThinQVocab` enum이 wire 표현을 담당. 공급자가 특정 값을 매핑 못 하면 어댑터가 거절.

---

## 4. 분기 2단계 — 중립 명령 → SmartThings capability

`POST /v1/devices/{id}/commands` body: `{"commands":[ {component:"main", capability, command, arguments} ... ]}`

| 중립 | 종류 | capability | command | arguments |
|---|---|---|---|---|
| `power=ON` | 전부 | `switch` | `on` | `[]` |
| `power=OFF` | 전부 | `switch` | `off` | `[]` |
| `mode=COOL` | 에어컨 | `airConditionerMode` | `setAirConditionerMode` | `["cool"]` |
| `mode=DRY` | 에어컨 | `airConditionerMode` | `setAirConditionerMode` | `["dry"]` |
| `mode=FAN` | 에어컨 | `airConditionerMode` | `setAirConditionerMode` | `["wind"]` |
| `mode=AUTO` | 에어컨 | `airConditionerMode` | `setAirConditionerMode` | `["auto"]` |
| `mode=AUTO` | 공기청정기 | `airPurifierFanMode` | `setAirPurifierFanMode` | `["auto"]` |
| `mode=SLEEP` | 공기청정기 | `airPurifierFanMode` | `setAirPurifierFanMode` | `["sleep"]` |
| `mode=TURBO` | 공기청정기 | `airPurifierFanMode` | `setAirPurifierFanMode` | `["high"]` `[근사]` |
| `mode=LOW` | 환풍기 | `fanSpeed` | `setFanSpeed` | `[1]` (정수) |
| `mode=MID` | 환풍기 | `fanSpeed` | `setFanSpeed` | `[2]` |
| `mode=HIGH` | 환풍기 | `fanSpeed` | `setFanSpeed` | `[3]` |
| `windDirection=FIXED` | 에어컨 | `fanOscillationMode` | `setFanOscillationMode` | `["fixed"]` |
| `windDirection=SWING` | 에어컨 | `fanOscillationMode` | `setFanOscillationMode` | `["all"]` |
| `temperature=25` | 에어컨 | `thermostatCoolingSetpoint` | `setCoolingSetpoint` | `[25]` |

(SmartThings 에어컨엔 `mode=AIRCLEAN` 매핑 없음 — LG 전용. 어댑터가 `SmartThingsApiException` → 502)

응답: `{"results":[{"id":"<uuid>","status":"ACCEPTED"}]}` (HTTP 200). 하나라도 `status != ACCEPTED` → 502.

---

## 5. 분기 2단계 — 중립 명령 → LG ThinQ resource (중첩)

`POST /devices/{id}/control` body: `{ "<resource>": { "<property>": <value> }, ... }` (변경 안 하는 resource 생략)

| 중립 | 종류 | resource | property | value |
|---|---|---|---|---|
| `power=ON` | 에어컨 | `operation` | `airConOperationMode` | `POWER_ON` |
| `power=ON` | 공기청정기 | `operation` | `airPurifierOperationMode` | `POWER_ON` `[근사]` |
| `power=ON` | 환풍기 | `operation` | `airFanOperationMode` | `POWER_ON` `[근사]` |
| `power=OFF` | 〃 | `operation` | 〃 | `POWER_OFF` |
| `mode=COOL` | 에어컨 | `airConJobMode` | `currentJobMode` | `COOL` |
| `mode=DRY` | 에어컨 | `airConJobMode` | `currentJobMode` | `AIR_DRY` |
| `mode=FAN` | 에어컨 | `airConJobMode` | `currentJobMode` | `FAN` |
| `mode=AUTO` | 에어컨 | `airConJobMode` | `currentJobMode` | `AUTO` |
| `mode=AIRCLEAN` | 에어컨 | `airConJobMode` | `currentJobMode` | `AIR_CLEAN` (LG 전용 — 공기청정) |
| `mode=AUTO` | 공기청정기 | `airPurifierJobMode` | `currentJobMode` | `AUTO` |
| `mode=SLEEP` | 공기청정기 | `airPurifierJobMode` | `currentJobMode` | `SLEEP` |
| `mode=TURBO` | 공기청정기 | `airPurifierJobMode` | `currentJobMode` | `CLEAN` `[근사]` |
| `mode=LOW/MID/HIGH` | 환풍기 | `airFlow` | `windStrength` | `LOW` / `MID` / `HIGH` |
| `windDirection=FIXED` | 에어컨 | `windDirection` | `rotateUpDown` | `false` (JSON boolean) |
| `windDirection=SWING` | 에어컨 | `windDirection` | `rotateUpDown` | `true` (LG는 상하 스윙 on/off) |
| `temperature=25` | 에어컨 | `temperature` | `targetTemperature` (+ `unit`:`"C"`) | `25` |

예 (에어컨, 전원+모드+온도 동시):
```json
{
  "operation":     { "airConOperationMode": "POWER_ON" },
  "airConJobMode": { "currentJobMode": "COOL" },
  "temperature":   { "targetTemperature": 25, "unit": "C" }
}
```

응답:
- 성공 `{ "messageId": "<uuid>", "timestamp": "2026-09-03T...Z", "response": {} }` (HTTP 200)
- 에러  `{ "error": { "code": "0110", "message": "..." } }` — `error != null` → 502

---

## 6. Front 조작 UI — 액추에이터 카드가 공급자별로 다름

CORE가 `(공급자, 종류)` 로 "이 액추에이터로 가능한 SELECT형 명령값"을 계산해 `ActuatorResponse.supportedValues`
(`{stateKey: [중립값...]}`) 로 내려준다. `panel.html` 은 그 목록대로 칩을 그림 — `if (LG)` 없음.

| | key | SmartThings 에어컨 | LG 에어컨 |
|---|---|---|---|
| 모드 | `mode` | `COOL` `DRY` `FAN` `AUTO` | `COOL` `DRY` `FAN` `AUTO` **`AIRCLEAN`** |
| 바람방향 | `windDirection` | `FIXED` `SWING` | `FIXED` `SWING` |

- 흐름: `ActuatorServiceImpl.toResponse` → `ProviderCommandCatalog.supportedValues(provider, type)` → `adapterRegistry.get(provider).supportedValues(type)` → `SmartThingsVocab`/`LgThinQVocab.supportedValues`
- 미연결(UNBOUND): `ProviderCommandCatalog.NEUTRAL` 로 폴백
- 플로우 에디터(룰엔진 노드)는 `ActuatorCommandPreset.forFlowNode` 로 `windDirection` 제외 (룰엔진 미지원)
- 실연동 시: `supportedValues` 를 하드코딩 대신 `GET /devices/{id}/profile`(LG) · `status.supportedAcModes`(ST) 응답으로 채우면 device 단위 정확도

---

## 7. 조회 엔드포인트 (실연동 대비 · 시뮬레이터 fixture)

| | SmartThings | LG ThinQ |
|---|---|---|
| 목록 | `GET /v1/devices` → `{"items":[{"deviceId","label","deviceTypeName",...}]}` | `GET /devices` → `[{"deviceId","deviceInfo":{"deviceType","alias","modelName"}}]` (순수 배열) |
| 상태 | `GET /v1/devices/{id}/status` → `{"components":{"main":{"<cap>":{"<attr>":{"value":...}}}}}` | `GET /devices/{id}/state` → `{"<resource>":{"<property>":...}}` |
| 프로파일 | (status 안 `supported*`) | `GET /devices/{id}/profile` → `{"<resource>":{"<property>":{"type":"enum","mode":["r","w"],"value":{"w":[...]}}}}` |

시뮬레이터는 deviceId 접두사(`st-aircon-…` / `lg-purifier-…`)로 종류를 인식해 종류별 fixture 응답.
CORE `listDevices()` 복원 — adapter interface + 두 client + `ProviderDevice`. 등록 UX는 자동생성 유지(B-1).

---

