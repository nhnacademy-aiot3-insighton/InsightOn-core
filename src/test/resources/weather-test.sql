-- 기존 테스트 데이터 초기화 (중복 방지)
DELETE
FROM locations
WHERE location_id IN (1, 2, 3);
DELETE
FROM groups
WHERE group_id IN (1);

-- 1. 날씨 테스트용 그룹 데이터
INSERT INTO groups (group_id, group_name, group_region, invite_token, created_at)
VALUES (1, '날씨 테스트 전용 그룹', '광주광역시 동구', 'weather-test-token', NOW());

-- 2. 날씨 API 테스트용 위치 데이터 (광주 동구 계림동 / X: 58, Y: 74)
INSERT INTO locations (location_id, group_id, location_name, auto_control_mode, created_at)
VALUES (1, 1, '광주 동구 사무실', 'SUGGESTION', NOW());

-- 3. 추가 테스트용 지역 데이터 (서울 중구 / X: 60, Y: 127)
INSERT INTO locations (location_id, group_id, location_name, auto_control_mode, created_at)
VALUES (2, 1, '서울 중구 본사', 'AI_DIRECT', NOW());

-- 4. 예외 테스트용 지역 데이터 (부산 해운대구 / X: 98, Y: 75)
INSERT INTO locations (location_id, group_id, location_name, auto_control_mode, created_at)
VALUES (3, 1, '부산 지사', 'SUGGESTION', NOW());