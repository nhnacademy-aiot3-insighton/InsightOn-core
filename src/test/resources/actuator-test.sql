SET
REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE groups RESTART IDENTITY;
TRUNCATE TABLE locations RESTART IDENTITY;
TRUNCATE TABLE actuators RESTART IDENTITY;
SET
REFERENTIAL_INTEGRITY TRUE;

INSERT INTO groups (group_id, group_name, description, group_region, invite_token, created_at)
VALUES (1, 'A회사', 'd', '광주', 'tA', CURRENT_TIMESTAMP),
       (2, 'B회사', 'd', '서울', 'tB', CURRENT_TIMESTAMP);

INSERT INTO locations (location_id, group_id, location_name, auto_control_mode, created_at)
VALUES (1, 1, '거실', 'AI_DIRECT', CURRENT_TIMESTAMP),
       (2, 1, '안방', 'AI_DIRECT', CURRENT_TIMESTAMP),
       (3, 1, '주방', 'AI_DIRECT', CURRENT_TIMESTAMP);

-- FORMAT JSON 구문을 추가하여 JSON 객체로 명시합니다.
-- 아래 3건은 테스트가 위치별/타입별 필터링을 검증할 수 있도록 위치 1엔 AIRCON 1건, 위치 2엔 AIRCON 1건,
-- 위치 3(검증 대상 밖)엔 AIR_PURIFIER 1건으로 분산되어 있음 - 위치 1/2를 합치면 AIRCON 총 2건.
INSERT INTO actuators (actuator_id, location_id, sensor_name, actuator_type, current_state, state_updated_at,
                       created_at)
VALUES (1, 1, '에어컨1', 'AIRCON', '{"power": "ON", "temperature": 24}' FORMAT JSON, CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

INSERT INTO actuators (actuator_id, location_id, sensor_name, actuator_type, current_state, state_updated_at,
                       created_at)
VALUES (2, 3, '거실 공기청정기 센서', 'AIR_PURIFIER', '{"power": "OFF", "mode": "AUTO"}' FORMAT JSON, CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

INSERT INTO actuators (actuator_id, location_id, sensor_name, actuator_type, current_state, state_updated_at,
                       created_at)
VALUES (3, 2, '에어컨2', 'AIRCON', '{"power": "OFF", "temperature": 22}' FORMAT JSON, CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);