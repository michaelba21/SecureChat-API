package com.securechat.mapper;

import com.securechat.dto.LoginRequest;

/**
 * Maps login DTOs (wrapper for consistency).
 */
public final class LoginDtoMapper {

    private LoginDtoMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static LoginRequest toLoginRequest(String email, String password) {
        return new LoginRequest(email, password);
    }
}

