--  Enforce chat_rooms.created_by integrity in a safe, idempotent way
-- This migration will:
--  1) Add a foreign key constraint on chat_rooms(created_by) -> users(id) if it does not exist
--  2) Attempt to set created_by to NOT NULL only if there are no NULL values
-- Both operations are guarded to avoid failing on existing data.

-- 1) Add FK constraint if missing
DO $$
BEGIN
  -- Check if foreign key constraint already exists
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
    WHERE tc.table_name = 'chat_rooms'
      AND tc.constraint_type = 'FOREIGN KEY'
      AND kcu.column_name = 'created_by'
  ) THEN
    -- Add foreign key constraint linking created_by to users.id
    ALTER TABLE chat_rooms
      ADD CONSTRAINT chat_rooms_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(id);
  END IF;
END$$;

-- 2) Set NOT NULL only when safe (no NULLs present)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM chat_rooms WHERE created_by IS NULL LIMIT 1) THEN
    -- If NULLs exist, log a notice instead of failing
    RAISE NOTICE 'Skipping ALTER TABLE SET NOT NULL for chat_rooms.created_by because NULL values exist.';
  ELSE
    -- Only set NOT NULL constraint when all rows have valid values
    EXECUTE 'ALTER TABLE chat_rooms ALTER COLUMN created_by SET NOT NULL';
  END IF;
END$$;
