-- 테이블 초기화 (순서 고려 및 FK 제약 해제)
SET
REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE sensor_attributes RESTART IDENTITY;
TRUNCATE TABLE sensors RESTART IDENTITY;
TRUNCATE TABLE gateways RESTART IDENTITY;
TRUNCATE TABLE groups RESTART IDENTITY;
SET
REFERENTIAL_INTEGRITY TRUE;

-- 1. 그룹 생성
INSERT INTO groups (group_id, group_name, description, group_region, invite_token, created_at)
VALUES (1, '회사', 'd', '광주', 't1', CURRENT_TIMESTAMP);

-- 2. 게이트웨이 생성
INSERT INTO gateways (gateway_id, group_id, gateway_name, protocol_type, connection_config, created_at, status)
VALUES (1, 1, 'GW1', 'MQTT', '{"ip":"1.1.1.1"}', CURRENT_TIMESTAMP, 'ACTIVE');

-- 3. 센서 생성 (sensor_id, sensor_name, sensor_eui 컬럼 사용)
INSERT INTO sensors (sensor_id, gateway_id, group_id, location_id, sensor_name, sensor_eui, created_at)
VALUES (1, 1, 1, NULL, '속성테스트센서', 'EUI-ATTR-1', CURRENT_TIMESTAMP);

-- 4. 센서 속성 생성 (sensor_attribute_id, sensor_id 컬럼 사용)
INSERT INTO sensor_attributes (sensor_attribute_id, sensor_id, group_id, metric_key, current_value_str)
VALUES (1, 1, 1, 'co2', '0'),
       (2, 1, 1, 'humidity', '0');