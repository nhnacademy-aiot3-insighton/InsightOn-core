SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE groups RESTART IDENTITY;
TRUNCATE TABLE locations RESTART IDENTITY;
TRUNCATE TABLE dashboards RESTART IDENTITY;
SET REFERENTIAL_INTEGRITY TRUE;

INSERT INTO groups (group_name, description, group_region, invite_token, created_at)
VALUES ('test-group', 'test-description', 'test-location', 'test-token', CURRENT_TIMESTAMP);

INSERT INTO locations (group_id, location_name, created_at, auto_control_mode)
VALUES (1, 'test-location', CURRENT_TIMESTAMP, 'SUGGESTION');

INSERT INTO dashboards (location_id, title, updated_at)
VALUES (1, 'test-dashboard', CURRENT_TIMESTAMP);
