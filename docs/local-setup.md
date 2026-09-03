# 로컬 실행 준비 — 액추에이터 어댑터 + 시뮬레이터

앱 5개(시뮬레이터 · core · front · ai)를 로컬에서 띄워 액추에이터 제어 흐름을 확인하기 위한 **최소 설정**만 정리.

| 앱 | 포트 | 프로파일 | DB |
|---|---|---|---|
| actuator-simulator | 8090 | 없음 | 없음 |
| InsightOn-core | 8300 | `local` | 로컬 postgres `core` 스키마 |
| InsightOn-front | 8400 | `local` | 없음 (core 호출) |
| InsightOn-ai | 8084 | `local` | 로컬 postgres `ai` 스키마 (제안 수락 테스트 시만) |

---

## 0. 사전 준비 — 로컬 도커 스택

`InsightOn-infra/docker-compose.yml` 로 컨테이너 4개를 띄운다.

| 컨테이너 | 포트 |
|---|---|
| `insighton-local-postgres` | 5432 |
| `insighton-local-redis` | 6379 |
| `insighton-local-rabbitmq` | 5672 (관리 UI 15672) |
| `insighton-local-influxdb` | 8087 → 8086 |

```bash
docker ps --format '{{.Names}}\t{{.Status}}' | grep insighton-local
```

## 1. DB 준비 (로컬 postgres `insighton` DB)

```sql
-- core: 액추에이터 공급자 컬럼 2개 (ddl-auto=validate 라 수동, 이미 했으면 스킵)
ALTER TABLE core.actuators ADD COLUMN control_provider    varchar(30);
ALTER TABLE core.actuators ADD COLUMN external_device_id  varchar(150);

-- ai 까지 로컬로 돌릴 때만 (테이블은 ddl-auto=update 가 자동 생성)
CREATE SCHEMA IF NOT EXISTS ai;
```

---

## 2. actuator-simulator (8090)

zip 을 풀어 IDE 에 임포트. `src/main/resources/application.properties` 는 이미 포함돼 있음:

```properties
server.port=8090
simulator.provider-token=local-sim-token
logging.level.com.insighton.actuatorsimulator=DEBUG
```

프로파일·DB 없음. 그냥 Run.

---

## 3. InsightOn-core — `local` 프로파일 (8300)

**파일**: `src/main/resources/application-local.properties` (없으면 아래 전문으로 생성)

```properties
spring.application.name=insighton-core
server.port=8300

# --- datasource (docker: insighton-local-postgres) ---
spring.datasource.url=jdbc:postgresql://localhost:5432/insighton?currentSchema=core
spring.datasource.username=insighton
spring.datasource.password=insighton
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true

# --- redis ---
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.database=0
telemetry.redis.host=localhost
telemetry.redis.port=6379
telemetry.redis.password=

# --- rabbitmq (guest/guest) ---
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# --- influxdb ---
influxdb.url=http://localhost:8087
influxdb.token=dev-local-insighton-token-000
influxdb.org=insighton
influxdb.bucket=core_telemetry

# --- 부팅만 통과하도록 더미 ---
service-url.auth=http://localhost:8000
weather.api.kma-base-url=https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0
weather.api.air-base-url=https://apis.data.go.kr/B552584/ArpltnInforInqireSvc
weather.api.kma-key=dummy
weather.api.air-key=dummy
management.tracing.sampling.probability=0.0

# --- 액추에이터 공급자 어댑터 → 로컬은 시뮬레이터 ---
actuator.smartthings.base-url=http://localhost:8090/smartthings
actuator.smartthings.token=local-sim-token
actuator.lg-thinq.base-url=http://localhost:8090/lg
actuator.lg-thinq.token=local-sim-token

logging.level.root=INFO
logging.level.com.insighton.core=DEBUG
```

**IntelliJ Run** → Edit Configurations → Active profiles = `local`
⚠️ `dev` 로 두면 팀 원격 DB(Tailscale)를 봐서 부팅 타임아웃.

핵심 4줄은 `actuator.*` — 없으면 `ActuatorRestClientConfig` 의 `@Value("${actuator.smartthings.base-url}")` 미해결로 부팅 실패.

---

## 4. InsightOn-front — `local` 프로파일 (8400)

**파일 1**: `src/main/resources/application-local.properties`

```properties
spring.application.name=insighton-front
server.port=8400

service-url.gateway=http://localhost:8300
server.forward-headers-strategy=framework
server.servlet.session.tracking-modes=cookie

management.endpoints.web.exposure.include=health
logging.level.root=INFO
logging.level.com.nhnacademy.insightonfront=DEBUG
```

**파일 2 (필수)**: `src/main/java/com/nhnacademy/insightonfront/config/LocalUserIdRequestInterceptor.java`
게이트웨이 없이 core 를 직접 부르면 `X-USER-ID` 헤더가 안 실려 core 가 400. 이 인터셉터가 `userId` 쿠키를 헤더로 옮겨준다 (`@Profile("local")`).

```java
package com.nhnacademy.insightonfront.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Profile("local")
public class LocalUserIdRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String userId = getCookie("userId");
        if (Objects.nonNull(userId)) {
            template.removeHeader("X-USER-ID");
            template.header("X-USER-ID", userId);
        }
    }

    private String getCookie(String name) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            Cookie[] cookies = attrs.getRequest().getCookies();
            if (Objects.nonNull(cookies)) {
                for (Cookie cookie : cookies) {
                    if (name.equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
        }
        return null;
    }
}
```

**IntelliJ Run** → Active profiles = `local`

**브라우저**: 로그인 흐름을 생략하므로 개발자도구 콘솔에서 쿠키를 직접 심는다 (core 로컬 DB 의 실제 매니저 계정/그룹):

```js
document.cookie = 'userId=3; path=/';
document.cookie = 'groupId=1; path=/';
```

---

## 5. InsightOn-ai — `local` 프로파일 (8084)  *(AI 제안 수락 경로를 테스트할 때만)*

기본 `dev` 프로파일은 `core-service.url=http://localhost:8081` + `config/dev/db.properties`(팀 원격 postgres)라 로컬 어댑터 테스트에 안 맞는다. 아래 `local` 프로파일을 새로 만든다.

**파일**: `src/main/resources/application-local.properties` (신규)

```properties
spring.application.name=insighton-ai
server.port=8084

# core(local, 8300) 직접 호출
core-service.url=http://localhost:8300
rule-engine-service.url=http://localhost:8081

# 로컬 postgres insighton DB 의 ai 스키마 (CREATE SCHEMA ai 선행, 테이블은 자동 생성)
spring.datasource.url=jdbc:postgresql://localhost:5432/insighton?currentSchema=ai
spring.datasource.username=insighton
spring.datasource.password=insighton
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# core 로컬 스택 공유
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# 부팅만 통과 — 제안 수락 경로는 LLM·influx 를 호출하지 않음
# (dummy 로 부팅이 안 되면 실제 GEMINI_API_KEY 값을 넣는다)
spring.ai.google.genai.api-key=dummy
influxdb.url=http://localhost:8087
influxdb.token=dev-local-insighton-token-000
influxdb.org=insighton
influxdb.bucket=core_telemetry

management.tracing.sampling.probability=0.0
logging.level.root=INFO
logging.level.com.insighton.ai=DEBUG
```

**IntelliJ Run** → Active profiles = `local`

> AI→core 경로 버그는 `InsightOn-ai` PR #115 로 수정됨 (경로에 `groups/{groupId}` 포함). 이전 버전이면 404.

---

## 6. 실행 순서

1. 도커 스택 (postgres/redis/rabbitmq/influxdb)
2. **actuator-simulator** (8090)
3. **InsightOn-core** — `local`, 8300
4. **InsightOn-front** — `local`, 8400  *(브라우저로 조작할 때)*
5. **InsightOn-ai** — `local`, 8084  *(제안 수락을 테스트할 때)*

---

## 7. 동작 확인

**시뮬레이터 단독** — 아무 deviceId 나 받고 `ACCEPTED` 면 정상:

```bash
curl -s -XPOST "http://localhost:8090/smartthings/v1/devices/ping/commands" \
  -H "Authorization: Bearer local-sim-token" -H "Content-Type: application/json" \
  -d '{"commands":[{"component":"main","capability":"switch","command":"on","arguments":[]}]}'
# → {"results":[{"id":"...","status":"ACCEPTED"}]}
```

**어댑터 흐름** — Front 에서 액추에이터를 **공급자를 골라** 등록(그래야 `control_provider` 채워짐) → 카드에서 전원/온도 클릭 → 로그 확인:

```bash
tail -f InsightOn-core/core-local.log        | grep -E '\[SmartThings\]|\[LG ThinQ\]'   # core 가 보낸 JSON
tail -f actuator-simulator/simulator-local.log | grep -E 'SMART_THINGS|LG_THINQ'          # 시뮬레이터가 받은 JSON
```

> `control_provider` 가 없는(공급자 미선택으로 등록한) 액추에이터는 `ActuatorControlFacade` 에서 400. 기존 것을 쓰려면:
> `UPDATE core.actuators SET control_provider='SMART_THINGS', external_device_id='st-aircon-1' WHERE actuator_id=<id>;`

상세 흐름: [`actuator-control-flow.md`](actuator-control-flow.md)
