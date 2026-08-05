SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE groups RESTART IDENTITY;
TRUNCATE TABLE gateways RESTART IDENTITY;
TRUNCATE TABLE sensor_devices RESTART IDENTITY;
TRUNCATE TABLE sensor_device_attributes RESTART IDENTITY;
SET REFERENTIAL_INTEGRITY TRUE;

INSERT INTO groups (group_id, group_name, description, group_region, invite_token, created_at)
VALUES (1, '회사', 'd', '광주', 't1', CURRENT_TIMESTAMP);

INSERT INTO gateways (gateway_id, group_id, gateway_name, protocol_type, connection_config, created_at, status)
VALUES (1, 1, 'GW1', 'MQTT', '{"ip":"1.1.1.1"}', CURRENT_TIMESTAMP, 'ACTIVE');

INSERT INTO sensors (device_id, gateway_id, group_id, location_id, device_type, device_name, device_eui, created_at)
VALUES (1, 1, 1, NULL, 'SENSOR', '속성테스트센서', 'EUI-ATTR-1', CURRENT_TIMESTAMP);

-- 테스트 코드의 hasSize(2) 검증에 맞게 속성 데이터를 2개만 삽입합니다.
INSERT INTO sensor_attributes (device_attribute_id, device_id, group_id, metric_key, current_value_str)
VALUES (1, 1, 1, 'co2', '0'),
       (2, 1, 1, 'humidity', '0');