-- I have fixed here the failed migration by removing it from history
DELETE FROM flyway_schema_history WHERE version = 5;

-- Verify the deletion
SELECT * FROM flyway_schema_history ORDER BY version;
