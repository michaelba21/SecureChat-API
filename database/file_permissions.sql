-- Add file_permissions table
CREATE TABLE IF NOT EXISTS file_permissions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  file_id UUID NOT NULL REFERENCES files(id),
  user_id UUID NOT NULL REFERENCES users(id),
  permission_type VARCHAR(50) NOT NULL,
  granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- Add composite index for permission lookups
CREATE INDEX idx_file_permissions_file ON file_permissions(file_id);
CREATE INDEX idx_file_permissions_user ON file_permissions(user_id);
