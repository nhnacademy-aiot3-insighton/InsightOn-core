SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE locations RESTART IDENTITY;
TRUNCATE TABLE group_members RESTART IDENTITY;
TRUNCATE TABLE groups RESTART IDENTITY;
SET REFERENTIAL_INTEGRITY TRUE;

INSERT INTO groups (group_name, description, group_region, invite_token, created_at)
VALUES ('test-group', 'test-description', 'test-location', 'test-token', CURRENT_TIMESTAMP);

INSERT INTO group_members (group_id, user_id, group_role, joined_at)
values (1, 1, 'MEMBER', CURRENT_TIMESTAMP);

INSERT INTO locations (group_id, location_name, created_at, auto_control_mode)
values (1, 'test-name', CURRENT_TIMESTAMP, 'SUGGESTION');