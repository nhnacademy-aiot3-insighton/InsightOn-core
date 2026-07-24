SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE groups RESTART IDENTITY;
TRUNCATE TABLE group_members RESTART IDENTITY;
SET REFERENTIAL_INTEGRITY TRUE;

INSERT INTO groups (group_name, description, location, invite_token, created_at)
VALUES ('test-group', 'test-description', 'test-location', 'test-token', CURRENT_TIMESTAMP);

INSERT INTO group_members (group_id, user_id, group_role, joined_at)
values (1, 1, 'MEMBER', CURRENT_TIMESTAMP)