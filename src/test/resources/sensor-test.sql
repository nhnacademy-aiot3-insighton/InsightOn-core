-- H2 기준 FK 제약조건 해제 및 테이블 초기화
SET
REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE groups RESTART IDENTITY;
TRUNCATE TABLE gateways RESTART IDENTITY;
TRUNCATE TABLE locations RESTART IDENTITY;
TRUNCATE TABLE sensors RESTART IDENTITY;
SET
REFERENTIAL_INTEGRITY TRUE;

-- 1. 그룹 2개 생성
INSERT INTO groups (group_id, group_name, description, group_region, invite_token, created_at)
VALUES (1, '테스트회사', '설명', '광주', 'token-1', CURRENT_TIMESTAMP),
       (2, '다른회사', '설명2', '서울', 'token-2', CURRENT_TIMESTAMP);

-- 2. 게이트웨이 2개 생성
INSERT INTO gateways (gateway_id, group_id, gateway_name, protocol_type, connection_config, created_at, status)
VALUES (1, 1, '게이트웨이1', 'MQTT', '{"ip":"192.168.0.1"}', CURRENT_TIMESTAMP, 'ACTIVE'),
       (2, 2, '게이트웨이2', 'MQTT', '{"ip":"192.168.0.2"}', CURRENT_TIMESTAMP, 'ACTIVE');

-- 3. 위치 1개 생성
INSERT INTO locations (location_id, group_id, location_name, auto_control_mode, created_at)
VALUES (1, 1, '4층 개발팀', 'SUGGESTION', CURRENT_TIMESTAMP);

-- 4. 센서 2개 생성 (sensors 테이블, sensor_id / sensor_name / sensor_eui 컬럼 사용)
INSERT INTO sensors (sensor_id, gateway_id, group_id, location_id, sensor_name, sensor_eui, created_at)
VALUES (1, 1, 1, NULL, '센서A', 'EUI-100', CURRENT_TIMESTAMP),
       (2, 2, 1, 1, '센서D', 'EUI-104', CURRENT_TIMESTAMP);