-- Create refresh_tokens table for storing JWT refresh tokens with security metadata
-- Refresh tokens allow users to obtain new access tokens without re-authenticating
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,                            -- Unique identifier for each token record
    token VARCHAR(512) NOT NULL UNIQUE,            -- The actual refresh token string (long for security)
                                                 
    user_id UUID NOT NULL,                         
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL, -- When token expires (with timezone awareness)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL, 
    last_used_at TIMESTAMP WITH TIME ZONE,         -- When token was last used to get new access token
                                                  
    ip_address VARCHAR(45),                        -- IP address where token was created (IPv4=15, IPv6=45 chars)
    user_agent VARCHAR(512),                     
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    -- Foreign key: ensures user exists, CASCADE deletes tokens when user is deleted
);

-- Create index on user_id for fast lookups of all tokens belonging to a specific user
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);


-- Used by: scheduled jobs that delete expired tokens, token validation checks
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expiry ON refresh_tokens(expiry_date);