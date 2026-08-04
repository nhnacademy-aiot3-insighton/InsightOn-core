-- ============================================================
-- InsightOn 전체 서비스 ERD DDL
-- PostgreSQL 문법. 서비스별 물리 DB는 완전히 분리되어 있으며,
-- 이 파일은 전체 스키마를 한눈에 보기 위한 참고용 통합본이다.
-- 물리 FK는 각 서비스 DB 내부 테이블끼리만 존재하고, 다른 서비스
-- 소유 테이블 참조는 전부 논리 키(컬럼만 존재, FK 제약 없음)이다.
-- ============================================================


-- ============================================================
-- 1. AUTH SERVICE DB
-- ============================================================

CREATE TABLE auth.users (
                            user_id                     BIGSERIAL     PRIMARY KEY,
                            email                       VARCHAR(300)  NOT NULL UNIQUE,
                            user_name                   VARCHAR(100)  NOT NULL,
                            phone_number                VARCHAR(60)   UNIQUE,
                            status                      VARCHAR(20)   NOT NULL,                 -- ACTIVE, SLEEP, WITHDRAW, BLOCK
                            last_login_at               TIMESTAMPTZ,
                            updated_at                  TIMESTAMPTZ   NOT NULL,
                            created_at                  TIMESTAMPTZ   NOT NULL,
                            withdrawn_at                TIMESTAMPTZ
);

CREATE TABLE auth.user_credentials (
                                       user_credential_id          BIGSERIAL     PRIMARY KEY,
                                       user_id                     BIGINT        NOT NULL UNIQUE,          -- @OneToOne, 유저당 1개
                                       password_hash               VARCHAR(255)  NOT NULL,
                                       updated_at                  TIMESTAMPTZ   NOT NULL,
                                       created_at                  TIMESTAMPTZ   NOT NULL,
                                       CONSTRAINT fk_user_credentials_user
                                           FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE auth.user_roles (
                                 user_role_id                BIGSERIAL     PRIMARY KEY,
                                 user_id                     BIGINT        NOT NULL,
                                 role                        VARCHAR(20)   NOT NULL,                 -- ADMIN, MEMBER
                                 created_at                  TIMESTAMPTZ   NOT NULL,
                                 CONSTRAINT fk_user_roles_user
                                     FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE auth.oauths (
                             oauth_id                    BIGSERIAL     PRIMARY KEY,
                             user_id                     BIGINT        NOT NULL,
                             provider                    VARCHAR(50)   NOT NULL,                 -- google/github
                             provider_user_id            VARCHAR(255)  NOT NULL,
                             created_at                  TIMESTAMPTZ   NOT NULL,
    -- 복합 UNIQUE 라 인라인 불가, 테이블 레벨로 유지
                             CONSTRAINT uq_oauths_provider_provider_user_id UNIQUE (provider, provider_user_id),
                             CONSTRAINT fk_oauths_user
                                 FOREIGN KEY (user_id) REFERENCES users (user_id)
);


-- ============================================================
-- 2. CORE SERVICE DB
-- ============================================================

CREATE TABLE core.groups (
                             group_id                    BIGSERIAL     PRIMARY KEY,
                             group_name                  VARCHAR(100)  NOT NULL,
                             description                 TEXT,
                             group_region                VARCHAR(50)   NOT NULL,
                             invite_token                VARCHAR(100)  NOT NULL,
                             created_at                  TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT uq_groups_invite_token UNIQUE (invite_token)
);

CREATE TABLE core.locations (
                                location_id                 BIGSERIAL     PRIMARY KEY,
                                group_id                    BIGINT        NOT NULL REFERENCES core.groups(group_id),
                                location_name                VARCHAR(100)  NOT NULL,
                                created_at                  TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
                                auto_control_mode           VARCHAR(50)   NOT NULL
);

CREATE TABLE core.group_members (
                                    group_member_id              BIGSERIAL    PRIMARY KEY,
                                    group_id                     BIGINT       NOT NULL REFERENCES core.groups(group_id),
                                    user_id                      BIGINT       NOT NULL,
                                    group_role                   VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
                                    joined_at                    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    CONSTRAINT uq_group_members_user_id UNIQUE (user_id)
);

CREATE TABLE core.gateways (
                               gateway_id                   BIGSERIAL    PRIMARY KEY,
                               group_id                     BIGINT       NOT NULL REFERENCES core.groups(group_id),
                               gateway_name                 VARCHAR(100) NOT NULL,
                               protocol_type                VARCHAR(30)  NOT NULL,
                               connection_config            JSONB        NOT NULL,
                               status                       VARCHAR(50)  NOT NULL,
                               created_at                   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               last_heartbeat_at            TIMESTAMPTZ,
                               CONSTRAINT uq_gateways_group_id UNIQUE (group_id)
);

CREATE TABLE core.sensors (
                              device_id                    BIGSERIAL    PRIMARY KEY,
                              gateway_id                   BIGINT       REFERENCES core.gateways(gateway_id),
                              location_id                  BIGINT       REFERENCES core.locations(location_id),
                              device_eui                   VARCHAR(50),
                              device_name                  VARCHAR(100) NOT NULL,
                              device_type                  VARCHAR(30)  NOT NULL,
                              last_seen_at                 TIMESTAMPTZ,
                              created_at                   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT uq_devices_device_eui UNIQUE (device_eui)
);

CREATE TABLE core.sensor_attributes (
                                        device_attribute_id          BIGSERIAL    PRIMARY KEY,
                                        device_id                    BIGINT       NOT NULL REFERENCES core.sensors(device_id),
                                        metric_key                   VARCHAR(50)  NOT NULL,
                                        current_value_str            VARCHAR(50),
                                        CONSTRAINT uq_device_attributes_device_metric UNIQUE (device_id, metric_key)
);

CREATE TABLE core.metric_definitions (
                                         metric_key                   VARCHAR(50)  PRIMARY KEY,               -- ex. co2, temperature, power_status, ac_mode
                                         metric_name                  VARCHAR(100) NOT NULL,                  -- ex. 이산화탄소, 습도
                                         unit                         VARCHAR(20)                             -- ex. °C, %, ppm, ppb, NULL
);

CREATE TABLE core.actuators (
                                actuator_id                  BIGSERIAL    PRIMARY KEY,
                                location_id                  BIGINT       NOT NULL REFERENCES core.locations(location_id),
                                device_name                  VARCHAR(100) NOT NULL,
                                actuator_type                VARCHAR(50)  NOT NULL,                  -- AIRCON, AIR_PURIFIER, VENTILATION_FAN
                                current_state                JSONB        NOT NULL,                  -- 현재 제어 상태(ON/OFF, 모드, 온도 등)
                                state_updated_at             TIMESTAMPTZ,
                                created_at                   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE core.actuator_run_logs (
                                        simulator_run_log_id         BIGSERIAL    PRIMARY KEY,
                                        actuator_id                  BIGINT       NOT NULL REFERENCES core.actuators(actuator_id),
                                        command_type                 VARCHAR(50)  NOT NULL,                  -- POWER_STATUS, OPERATION_MODE, SET_TEMPERATURE
                                        command_value                VARCHAR(50)  NOT NULL,                  -- ON, OFF, TURBO, COOL, 24.0 등
                                        executed_by_type             VARCHAR(50)  NOT NULL,                  -- USER, AI_SYSTEM, RULE_ENGINE
                                        executed_by_user_id          BIGINT,
                                        executed_at                  TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE core.dashboards (
                                 dashboard_id                 BIGSERIAL    PRIMARY KEY,
                                 location_id                  BIGINT       NOT NULL REFERENCES core.locations(location_id),
                                 title                        VARCHAR(100) NOT NULL,
                                 updated_at                   TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP,
                                 CONSTRAINT uq_dashboards_location UNIQUE (location_id)
);

CREATE TABLE core.widgets (
                              widget_id                    BIGSERIAL    PRIMARY KEY,
                              dashboard_id                 BIGINT       NOT NULL REFERENCES core.dashboards(dashboard_id),
                              location_id                  BIGINT       NOT NULL REFERENCES core.locations(location_id),
                              type                         VARCHAR(50)  NOT NULL,
                              metric_key                   VARCHAR(50)  NOT NULL,
                              x_pos                        INT          NOT NULL,
                              y_pos                        INT          NOT NULL,
                              width                        INT          NOT NULL,
                              height                       INT          NOT NULL,
                              widget_config                JSONB        NOT NULL
);



-- ============================================================
-- 3. RULE ENGINE SERVICE DB
-- ============================================================



CREATE TABLE engine.flows (
                              flow_id                      BIGSERIAL    PRIMARY KEY,
                              group_id                     BIGINT       NOT NULL,                  -- 논리 키 (Core, 물리 FK 없음)
                              location_id                  BIGINT       NOT NULL,                  -- 논리 키 (Core, 물리 FK 없음)
                              name                         VARCHAR(100) NOT NULL,
                              description                  TEXT,
                              status                       VARCHAR(20)  NOT NULL,                  -- ACTIVE, INACTIVE, ARCHIVED
                              created_at                   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE engine.nodes (
                              node_id                      BIGSERIAL    PRIMARY KEY,
                              flow_id                      BIGINT       NOT NULL REFERENCES flows(flow_id),
                              node_type                    VARCHAR(50)  NOT NULL,                  -- TRIGGER_NODE, FILTER_NODE, ACTION_NODE
                              configuration                JSONB        NOT NULL                   -- ex. {"expression": "co2 > 1500"}
);

CREATE TABLE engine.links (
                              link_id                      BIGSERIAL    PRIMARY KEY,
                              flow_id                      BIGINT       NOT NULL REFERENCES flows(flow_id),
                              source_node_id                BIGINT       NOT NULL REFERENCES nodes(node_id),
                              target_node_id                BIGINT       NOT NULL REFERENCES nodes(node_id),
                              source_port                  VARCHAR(50)  NOT NULL,
                              target_port                  VARCHAR(50)  NOT NULL
);


-- ============================================================
-- 4. AI / 분석 SERVICE DB
-- ============================================================

CREATE TABLE ai.hourly_telemetry_stats (
                                           hourly_telemetry_stat_id     BIGSERIAL    PRIMARY KEY,
                                           location_id                  BIGINT       NOT NULL,                  -- 논리 키 (Core), group_id 컬럼 없음
                                           log_hour                     TIMESTAMPTZ  NOT NULL,
                                           metrics_avg                  JSONB        NOT NULL,
                                           metrics_max                  JSONB        NOT NULL,
                                           metrics_min                  JSONB        NOT NULL,
                                           actuator_on_minutes          JSONB,
                                           created_at                   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           CONSTRAINT uq_hourly_telemetry_stats_location_hour UNIQUE (location_id, log_hour)
);

CREATE TABLE ai.reports (
                            report_id                    BIGSERIAL    PRIMARY KEY,
                            group_id                     BIGINT       NOT NULL,                  -- 논리 키 (Core)
                            location_id                  BIGINT       NOT NULL,                  -- 논리 키 (Core)
                            title                        VARCHAR(200) NOT NULL,
                            report_type                  VARCHAR(20)  NOT NULL,                  -- WEEKLY, MONTHLY
                            content                      TEXT         NOT NULL,
                            created_at                   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai.suggestion_logs (
                                    suggestion_log_id            BIGSERIAL    PRIMARY KEY,
                                    group_id                     BIGINT       NOT NULL,                  -- 논리 키 (Core)
                                    location_id                  BIGINT       NOT NULL,                  -- 논리 키 (Core)
                                    title                        VARCHAR(255) NOT NULL,
                                    suggestion_text              TEXT         NOT NULL,
                                    action_payload               JSONB        NOT NULL,
                                    is_accepted                  BOOLEAN      DEFAULT NULL,               -- null=대기, true=수락, false=거절
                                    created_at                   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai.engine_alerts (
                                  engine_alert_id               BIGSERIAL   PRIMARY KEY,
                                  group_id                      BIGINT      NOT NULL,                  -- 논리 키 (Core)
                                  location_id                   BIGINT      NOT NULL,                  -- 논리 키 (Core)
                                  flow_id                       BIGINT      NOT NULL,                  -- 논리 키 (Rule Engine), 항상 값 존재
                                  title                         VARCHAR(200) NOT NULL,
                                  message                       TEXT        NOT NULL,
                                  severity                      VARCHAR(20) NOT NULL,                  -- INFO, WARNING, CRITICAL
                                  trigger_value                 NUMERIC(10,2),
                                  created_at                    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai.dashboard_notifications (
                                            dashboard_notification_id     BIGSERIAL   PRIMARY KEY,
                                            group_id                      BIGINT      NOT NULL,                  -- 논리 키 (Core)
                                            location_id                   BIGINT      NOT NULL,                  -- 논리 키 (Core)
                                            notification_type             VARCHAR(30) NOT NULL,                  -- ENGINE_ALERT / SUGGESTION / REPORT
                                            source_id                     BIGINT      NOT NULL,                  -- 딥링크용, 물리 FK 불가
                                            title                         VARCHAR(255) NOT NULL,
                                            created_at                    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            is_read                       BOOLEAN     NOT NULL DEFAULT FALSE
);