-- Add chat_room_members table
CREATE TABLE IF NOT EXISTS chat_room_members (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  chat_room_id UUID NOT NULL REFERENCES chat_rooms(id),
  user_id UUID NOT NULL REFERENCES users(id),
  joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  left_at TIMESTAMP,
  is_active BOOLEAN NOT NULL DEFAULT true,
  UNIQUE(chat_room_id, user_id)
);
-- Add index for active membership queries
CREATE INDEX idx_chat_room_members_room ON chat_room_members(chat_room_id);
CREATE INDEX idx_chat_room_members_user ON chat_room_members(user_id);
