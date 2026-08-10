SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE groups RESTART IDENTITY;
TRUNCATE TABLE gateways RESTART IDENTITY;
TRUNCATE TABLE sensors RESTART IDENTITY;
TRUNCATE TABLE sensor_attributes RESTART IDENTITY;
SET REFERENTIAL_INTEGRITY TRUE;

INSERT INTO groups (group_id, group_name, description, group_region, invite_token, created_at)
VALUES (1, '회사', 'd', '광주', 't1', CURRENT_TIMESTAMP);

INSERT INTO gateways (gateway_id, group_id, gateway_name, protocol_type, connection_config, created_at, status)
VALUES (1, 1, 'GW1', 'MQTT', CAST('{"ip":"1.1.1.1"}' AS JSON), CURRENT_TIMESTAMP, 'ACTIVE');

INSERT INTO sensors (sensor_id, gateway_id, group_id, location_id, sensor_name, sensor_eui, created_at)
VALUES (1, 1, 1, NULL, '속성테스트센서', 'EUI-ATTR-1', CURRENT_TIMESTAMP);

-- group_id, current_value_str 컬럼 제거됨 - 지금 테이블엔 sensor_attribute_id/sensor_id/metric_key 3개뿐
INSERT INTO sensor_attributes (sensor_attribute_id, sensor_id, metric_key)
VALUES (1, 1, 'co2'),
       (2, 1, 'humidity');