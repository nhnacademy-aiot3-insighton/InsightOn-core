SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE groups RESTART IDENTITY;
SET REFERENTIAL_INTEGRITY TRUE;

INSERT INTO groups (group_name, description, group_region, invite_token, created_at)
VALUES ('test-group', 'test-description', 'test-location', 'test-token', CURRENT_TIMESTAMP);
