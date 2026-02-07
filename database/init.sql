-- PostgreSQL initialization script for SecureChat API
-- users: core user table with authentication and profile data
CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username VARCHAR(255) UNIQUE NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login TIMESTAMP,
  is_active BOOLEAN NOT NULL DEFAULT true
);
-- chat_rooms: containers for group conversations with privacy controls
CREATE TABLE IF NOT EXISTS chat_rooms (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) NOT NULL,
  description TEXT,
  created_by UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_private BOOLEAN NOT NULL DEFAULT false,
  max_participants INTEGER NOT NULL DEFAULT 50
);
-- messages: individual chat messages with edit/delete tracking
CREATE TABLE IF NOT EXISTS messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  chat_room_id UUID NOT NULL REFERENCES chat_rooms(id),
  content TEXT NOT NULL,
  message_type VARCHAR(50) NOT NULL DEFAULT 'TEXT',
  timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_edited BOOLEAN NOT NULL DEFAULT false,
  edited_at TIMESTAMP,
  is_deleted BOOLEAN NOT NULL DEFAULT false
);
-- files: uploaded file storage with metadata
CREATE TABLE IF NOT EXISTS files (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  filename VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_size BIGINT NOT NULL,
  mime_type VARCHAR(100) NOT NULL,
  uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_public BOOLEAN NOT NULL DEFAULT false
);

-- performance indexes for common query patterns
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_messages_chat_room ON messages(chat_room_id);
CREATE INDEX idx_messages_user ON messages(user_id);
CREATE INDEX idx_messages_timestamp ON messages(timestamp);
CREATE INDEX idx_files_user ON files(user_id);
CREATE INDEX idx_chat_rooms_created_by ON chat_rooms(created_by);
