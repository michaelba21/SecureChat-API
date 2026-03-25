-- Add a roles column to the users table to store multiple user roles/permissions
-- This enables role-based access control (RBAC) for user authorization
ALTER TABLE users ADD COLUMN roles VARCHAR(255)[] DEFAULT ARRAY['ROLE_USER'];