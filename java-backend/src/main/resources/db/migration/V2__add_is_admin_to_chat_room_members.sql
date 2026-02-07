-- Add administrator flag to chat room members table
-- This allows distinguishing regular members from administrators within a chat room
ALTER TABLE chat_room_members
ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT false;
