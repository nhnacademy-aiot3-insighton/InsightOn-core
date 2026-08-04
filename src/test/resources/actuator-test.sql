SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE groups RESTART IDENTITY;
TRUNCATE TABLE locations RESTART IDENTITY;
TRUNCATE TABLE actuator_run_logs RESTART IDENTITY;
TRUNCATE TABLE actuators RESTART IDENTITY;
SET REFERENTIAL_INTEGRITY TRUE;

INSERT INTO groups (group_id, group_name, description, group_region, invite_token, created_at)
VALUES (1, 'A회사', 'd', '광주', 'tA', CURRENT_TIMESTAMP),
       (2, 'B회사', 'd', '서울', 'tB', CURRENT_TIMESTAMP);

INSERT INTO locations (location_id, group_id, location_name, auto_control_mode, created_at)
VALUES (1, 1, 'A-1층', 'SUGGESTION', CURRENT_TIMESTAMP),
       (2, 2, 'B-1층', 'SUGGESTION', CURRENT_TIMESTAMP);

--  위치 1번(A회사)과 위치 2번(B회사)에 각각 1개씩만 남기도록 공청기를 제거하거나 분리합니다.
INSERT INTO actuators (actuator_id, location_id, device_name, actuator_type, current_state, created_at)
VALUES (1, 1, '에어컨1', 'AIRCON', '{"power":"OFF"}', CURRENT_TIMESTAMP),
       (2, 2, '에어컨2', 'AIRCON', '{"power":"OFF"}', CURRENT_TIMESTAMP);