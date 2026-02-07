-- Users table: stores application user accounts and authentication information
CREATE TABLE users (
    id UUID PRIMARY KEY,                     -- Unique identifier for each user (UUID for security)
    username VARCHAR(255) NOT NULL UNIQUE,  
    email VARCHAR(255) NOT NULL UNIQUE,      -- Email address for login/communication, must be unique
    password_hash VARCHAR(255) NOT NULL,     -- Hashed password (never store plaintext passwords!)
    created_at TIMESTAMP NOT NULL,           
    last_login TIMESTAMP,                 
    is_active BOOLEAN NOT NULL DEFAULT true  -- Soft delete flag: false = deactivated account
);

-- Chat rooms table: stores chat room metadata and configuration
CREATE TABLE chat_rooms (
    id UUID PRIMARY KEY,                     
    name VARCHAR(255) NOT NULL,              -- Display name of the chat room
    description TEXT,                        
    max_participants INT,                    -- Maximum number of users allowed (null = unlimited)
    is_private BOOLEAN NOT NULL DEFAULT false, 
    created_by UUID NOT NULL REFERENCES users(id), -- User who created the room (foreign key)
    created_at TIMESTAMP NOT NULL           
);

-- Chat room members table: many-to-many relationship between users and chat rooms
CREATE TABLE chat_room_members (
    id UUID PRIMARY KEY,                     -- Unique identifier for each membership record
    chat_room_id UUID NOT NULL REFERENCES chat_rooms(id), -- Which chat room (foreign key)
    user_id UUID NOT NULL REFERENCES users(id), 
    joined_at TIMESTAMP NOT NULL,            -- When user joined the chat room
    left_at TIMESTAMP,                       
    is_active BOOLEAN NOT NULL DEFAULT true, -- Current membership status (soft delete)
    UNIQUE(chat_room_id, user_id)            
    -- Note: A user can be a member of multiple rooms, but only once per room
);

-- Messages table: stores all chat messages with metadata
CREATE TABLE messages (
    id UUID PRIMARY KEY,                     -- Unique identifier for each message
    chat_room_id UUID NOT NULL REFERENCES chat_rooms(id), -- Which chat room contains this message
    user_id UUID NOT NULL REFERENCES users(id), -- Who sent the message
    content TEXT NOT NULL,                
    message_type VARCHAR(50) NOT NULL DEFAULT 'TEXT',
    created_at TIMESTAMP NOT NULL,           
    is_edited BOOLEAN NOT NULL DEFAULT false, -- Flag: true if message was edited after sending
    edited_at TIMESTAMP,                   
    is_deleted BOOLEAN NOT NULL DEFAULT false -- Soft delete: true = message deleted (content hidden)
    -- Messages are kept for history even when deleted (soft delete)
);

-- Files table: stores metadata for uploaded files (actual files stored on disk/cloud)
CREATE TABLE files (
    id UUID PRIMARY KEY,                     -- Unique identifier for each file record
    user_id UUID NOT NULL REFERENCES users(id), 
    filename VARCHAR(255) NOT NULL,          -- Original filename from user's device
    file_path TEXT NOT NULL,                
    file_size BIGINT NOT NULL,               -- File size in bytes
    mime_type VARCHAR(255) NOT NULL,        
    is_public BOOLEAN NOT NULL DEFAULT false, -- Visibility: true = accessible without permissions
    uploaded_at TIMESTAMP NOT NULL          
    -- Note: Actual file content is stored separately (filesystem/S3), not in database
);

-- File permissions table: controls which users can access which files
CREATE TABLE file_permissions (
    id UUID PRIMARY KEY,                     -- Unique identifier for each permission record
    file_id UUID NOT NULL REFERENCES files(id), 
    user_id UUID NOT NULL REFERENCES users(id), 
    permission_type VARCHAR(50) NOT NULL,    -- Type: READ, WRITE, DELETE, SHARE, etc.
    granted_at TIMESTAMP NOT NULL,         
    UNIQUE(file_id, user_id)                 -- Prevent duplicate permissions for same user+file
    --  Each user can have only one permission record per file
);