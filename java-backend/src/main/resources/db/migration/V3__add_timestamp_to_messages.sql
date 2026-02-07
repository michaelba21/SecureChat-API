-- Add a timestamp column to the messages table for when the message was sent
-- This column provides an alternative or redundant timestamp to the existing 'created_at' column
ALTER TABLE messages
ADD COLUMN timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP; -- New column: when message was sent
