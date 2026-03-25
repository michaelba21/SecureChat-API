-- V8__rename_messages_user_id_to_sender_id.sql

-- First check if user_id exists and rename it to sender_id
DO $$
BEGIN
    -- Check if user_id column exists in messages table
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'messages' AND column_name = 'user_id'
    ) THEN
        -- First, drop any existing foreign key constraint
        ALTER TABLE messages DROP CONSTRAINT IF EXISTS messages_user_id_fkey;
        
        -- Rename the column
        ALTER TABLE messages RENAME COLUMN user_id TO sender_id;
        
        -- Re-add the foreign key constraint
        ALTER TABLE messages 
        ADD CONSTRAINT fk_messages_sender 
        FOREIGN KEY (sender_id) REFERENCES users(id);
    END IF;
END $$;