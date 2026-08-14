# 세션 인수인계 노트 (Claude Code)

이 파일은 컨텍스트가 꽉 찬 이전 세션의 작업 내역을 새 창(새 세션)에서 이어갈 수 있게 정리한 노트입니다.
새 세션 시작할 때 "HANDOFF.md 읽고 시작해줘" 라고 하면 됩니다. 다 읽고 나면 이 파일은 지우거나 무시해도 됩니다 (프로젝트 문서 아님, 커밋 안 해도 됨).

## 프로젝트/환경 배경
- 브랜치: `feature/sensor-refect`
- 로컬 Postgres: IntelliJ Database 콘솔로 접속, DB명 `insighton`, 스키마 `public` (⚠️ `application-dev.properties`에 박힌 `s3.java21.net:8000/aiot3-team3-project?currentSchema=core`는 원격 공유 서버라 로컬과 다름 — 로컬 실행 시 datasource를 env var로 오버라이드해서 씀, 정확한 값은 사용자만 앎)
- 앱은 사용자가 IntelliJ에서 직접 띄움 (Claude가 대신 못 띄움, 포트 8080)
- Redis는 로컬에서 도는 중. RabbitMQ/로컬 InfluxDB(`core_telemetry` 버킷)는 세션 초반엔 안 떠있었음
- 실데이터가 있는 InfluxDB는 로컬(`core_telemetry`, 비어있음)과 별개의 인스턴스 - 사용자가 스크린샷으로 보여줬고 정확한 접속 URL은 미확인. 여기서 실제 센서 10개(EUI/이름/메트릭)를 확인함 (Milesight EM300-TH/AM103/AM107/EM500-CO2 계열)

## 이번 세션에서 한 일 (시간순)

### 1. `.http` 파일 버그 수정 (커밋됨: `4bd9efa`, `7411fa9`)
- `actuator.http`의 모든 요청이 정의 안 된 `{{groupId10}}` 변수 씀 → `{{groupId}}`로 수정
- `sensorAttribute.http`의 메트릭 정의 등록이 `PUT /internal/metric-definitions`로 잘못돼있었음 (실제는 `POST /internal/v1/metric-definitions`) → 수정
- `groupmember.http` 끝에 IntelliJ 자동생성 예제 요청 삭제
- `actuator-ai.http`의 실행로그 조회 날짜 범위(`to=2026-08-09`)가 과거라 오늘 로그가 잘림 → `to=2027-08-09`로 넉넉히 늘림

### 2. `SensorServiceImpl.updateSensor`의 `@Transactional` 누락 버그 수정 (커밋됨)
- 증상: `PUT /api/v1/sensor/{id}`가 204 반환하는데 DB에 위치/이름이 실제로는 안 바뀜
- 원인: 클래스 레벨 `@Transactional(readOnly = true)`인데 `updateSensor` 메서드에만 `@Transactional` 오버라이드가 빠져있어서 Hibernate가 flush를 안 함
- 이미 고쳐서 커밋됨 - **이 부분은 손 안 대도 됨**

### 3. 액추에이터 장소 삭제 캐스케이드 (커밋됨)
- `LocationUseCase.deleteLocation()`이 센서는 detach 하는데 액추에이터(location_id NOT NULL FK라 못 detach)는 아예 안 지워서 FK 위반 위험 있었음
- `ActuatorService.deleteAllByLocationId(Long locationId)` 추가하고 `LocationUseCase.deleteLocation()`에서 호출하도록 수정 - **완료, 커밋됨**

### 4. `locationId` → `locationName`으로 변경 (커밋됨: `7411fa9`)
- 사용자가 `locationId`를 모르는 상태로 액추에이터 생성/센서 수정 요청을 만들어야 하는 문제
- `ActuatorRequest.locationId` → `locationName`, `SensorUpdateRequest.locationId` → `locationName`
- `LocationRepository.findByGroupGroupIdAndLocationName(groupId, locationName)` 신규 추가
- `LocationNotFoundException.notFoundLocationByName(String)` 신규 추가
- `ActuatorServiceImpl.createActuator`, `SensorServiceImpl.updateSensor` 내부에서 그룹 스코프로 이름 조회하도록 변경
- **완료, 커밋됨 - 손 안 대도 됨**

### 5. 센서 위치 변경 시 캐시-DB 정합성 버그 수정, 애프터커밋 패턴 적용 (커밋 안 됨, 워킹트리에 있음)
- 증상: `updateSensor`에서 위치 변경 후 이름 검증(`newSensorName.isBlank()`)에서 예외가 나 롤백되면, 캐시(`SensorLookupCacheService`)는 이미 바뀐 값으로 갱신된 채 DB만 롤백되는 정합성 버그
- 기존 `ActuatorStatusEventListener` 패턴(`@TransactionalEventListener(phase = AFTER_COMMIT)`)을 그대로 재사용
- 신규 파일: `domain/sensors/event/SensorCacheSyncEvent.java`, `adapter/mqtt/cache/SensorCacheEventListener.java`
- `SensorServiceImpl.updateSensorLocation`/`updateSensor`가 캐시 직접 갱신 대신 `eventPublisher.publishEvent(new SensorCacheSyncEvent(...))` 발행하도록 변경
- **완료, 테스트 통과 확인함, 아직 커밋 안 됨 (워킹트리 상태로 남아있음) - 유지하기로 함**

### 6. `sensor_attributes` 자동등록 안 되는 문제 조사 → 롤백됨
- 원인 조사: ①`metric_definitions`가 자동으로 안 채워짐(수동 INSERT 필요), ②`SensorServiceImpl.autoProvision`이 센서 최초 등록 순간에만 속성을 채우고 이후엔 재동기화 안 함
- `MetricDefinitionSeeder`(ApplicationRunner) + `SensorService.syncAttributes()` 구현했었으나, **팀원이 "metric_definitions 미리 인서트하면 된다"는 더 단순한 진단을 내려서 사용자가 전체 롤백 요청 → 완전히 되돌림 (지금 코드에 없음)**
- 필요하면 이 대화 트랜스크립트에 전체 설계 있음 (재구현 가능)

### 7. Sensor/Actuator/SensorAttribute UseCase 레이어 추출 → 전체 롤백됨
- `LocationUseCase`/`GroupUseCase`처럼 권한체크(`validateManagerRole`/`validateGroupMembership`)를 Service에서 분리해서 `ActuatorUseCase`/`SensorUseCase`/`SensorAttributeUseCase`로 옮기는 작업
- `SensorUseCase`는 URL이 group-scoped가 아니라서 `SensorService.getSensorGroupId(sensorId)`를 새로 만들어서 권한체크 전에 그룹을 알아내는 패턴 사용
- 완전히 구현하고 테스트까지 다 통과시켰었으나(241 tests), **사용자가 "수정하지 말고 보여주기만 하라"고 한 뒤 리뷰 도중 사용자가 직접(혹은 IDE가) `GroupUseCase.java`에 이상한 문자(`ㅊ`)를 넣은 걸 제가 고쳤다고 오해해서 "전체 롤백해달라"고 요청 → 완전히 롤백함**
- **지금 `ActuatorUseCase.java`/`SensorUseCase.java`/`SensorAttributeUseCase.java`는 사용자가 미리 만들어둔 빈 스텁(`public class X {}`) 상태로 되돌아가 있음**
- 사용자가 나중에 이 작업을 다시 하고 싶다고 하면, 이 대화 트랜스크립트에 전체 설계와 구현이 다 있으니 그대로 재현 가능 (Actuator는 깔끔했고, Sensor/SensorAttribute는 `getSensorGroupId` 패턴 필요했음)

## 현재 git 상태 (이 파일 쓰는 시점 기준)
- 최근 커밋 5개: `7411fa9`(locationId→locationName), `86419d0`(테스트 수정), `4bd9efa`(http 작성), `1f2d85c`, `5633d8a`
- 워킹트리에 커밋 안 된 변경사항 있음 (전부 `git status`로 확인 가능):
  - `SensorServiceImpl`/`SensorService` 등: 애프터커밋 이벤트 패턴 (섹션 5, 유지하기로 한 것)
  - `SensorCacheEventListener.java`, `domain/sensors/event/` (신규, untracked)
  - `ActuatorUseCase.java`/`SensorUseCase.java`/`SensorAttributeUseCase.java` (신규, untracked, 빈 스텁 상태)
  - `ActuatorCommandPreset.java`: 이건 애초에 사용자가 세션 시작 전부터 갖고 있던 변경사항, Claude가 손 안 댐
- **git add 되어있던(staged) 게 방금 unstage 처리됨** - 예전에 UseCase 리팩토링 버전이 스테이징된 채 남아있어서 그대로 커밋하면 롤백한 게 되살아날 뻔했음. 지금은 staged 없이 전부 워킹트리 변경사항으로만 존재.

## 빌드/테스트 상태
- `./mvnw -o clean test` → **224 tests, 0 failures, 32 skipped** (정상, 마지막 확인 시점 기준)
- 여러 `@Disabled` 테스트 클래스 있음 (`SensorServiceTest`, `ActuatorServiceTest`, `SensorAttributeServiceTest`) - 원래부터 비활성 상태, 손 안 댐(시그니처만 컴파일되게 맞춰둠)

## 아직 안 한 것 / 다음에 물어볼 만한 것
- Gateway 생성 요청(`GatewayCreateRequest.groupsId`)도 다른 API들처럼 URL 경로 기반으로 바꿀지 여부 (locationId→Name 작업 때 발견했지만 손 안 댐)
- MQTT 실기기 연동 테스트는 계속 보류 상태 (SQL INSERT로 대체 중)
- UseCase 리팩토링을 다시 할지 여부 - 롤백은 했지만 사용자가 원래 이 방향으로 가고 싶어했던 것 같음 (빈 스텁 파일을 미리 만들어뒀었음)
