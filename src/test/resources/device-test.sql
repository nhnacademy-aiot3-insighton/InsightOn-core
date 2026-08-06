SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE groups RESTART IDENTITY;
TRUNCATE TABLE gateways RESTART IDENTITY;
TRUNCATE TABLE locations RESTART IDENTITY;
TRUNCATE TABLE sensor_devices RESTART IDENTITY;
SET REFERENTIAL_INTEGRITY TRUE;

-- 그룹을 2개 생성합니다.
INSERT INTO groups (group_id, group_name, description, group_region, invite_token, created_at)
VALUES (1, '테스트회사', '설명', '광주', 'token-1', CURRENT_TIMESTAMP),
       (2, '다른회사', '설명2', '서울', 'token-2', CURRENT_TIMESTAMP);

-- 각 게이트웨이가 서로 다른 group_id(1번, 2번)를 가지도록 분리합니다.
INSERT INTO gateways (gateway_id, group_id, gateway_name, protocol_type, connection_config, created_at, status)
VALUES (1, 1, '게이트웨이1', 'MQTT', '{"ip":"192.168.0.1"}', CURRENT_TIMESTAMP, 'ACTIVE'),
       (2, 2, '게이트웨이2', 'MQTT', '{"ip":"192.168.0.2"}', CURRENT_TIMESTAMP, 'ACTIVE');

INSERT INTO locations (location_id, group_id, location_name, auto_control_mode, created_at)
VALUES (1, 1, '4층 개발팀', 'SUGGESTION', CURRENT_TIMESTAMP);

INSERT INTO sensor_devices (device_id, gateway_id, group_id, location_id, device_type, device_name, device_eui, created_at)
VALUES (1, 1, 1, NULL, 'SENSOR', '센서A', 'EUI-100', CURRENT_TIMESTAMP),
       (2, 2, 1, 1,    'SENSOR', '센서D', 'EUI-104', CURRENT_TIMESTAMP);