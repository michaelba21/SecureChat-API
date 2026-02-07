-- Add test user with credentials: testuser / password123
-- Password hash is bcrypt encoded for "password123"
INSERT INTO users (username, email, password_hash, created_at, is_active)
VALUES ('testuser', 'testuser@securechat.com', '$2a$10$slYQmyNdGzin7olVN3p5Be7DkH0rImlNCez0jHMVrD2KmF2P6MLKO', CURRENT_TIMESTAMP, true)
ON CONFLICT (username) DO NOTHING;
