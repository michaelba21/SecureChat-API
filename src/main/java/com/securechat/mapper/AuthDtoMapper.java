package com.securechat.mapper;

import com.securechat.dto.AuthRequest;
import com.securechat.dto.AuthResponse;
import com.securechat.dto.AuthResponse.UserInfo;
import com.securechat.entity.User;

/**
 * Utility class for converting between User entities and authentication DTOs.
 * This follows the DTO pattern to separate database entities from API responses.
 */
public final class AuthDtoMapper {

    // Private constructor to prevent instantiation (utility class pattern)
    private AuthDtoMapper() {}

    /**
     * Converts a User entity to a UserInfo DTO.
     
     * @param user The User entity to convert (can be null)
     * @return UserInfo DTO containing id, username, and email, or null if input is null
     */
    public static UserInfo toUserInfo(User user) {
        if (user == null) {
            return null; // Handle null input gracefully
        }
        return new UserInfo(
                user.getId() != null ? user.getId().toString() : null, // Convert UUID to String for JSON
                user.getUsername(),
                user.getEmail());
    }

    /**
     * Creates a complete authentication response DTO.
     * @param accessToken JWT access token for API authentication
     * @param refreshToken 
     * @param message Success or informational message
     * @param user 
     * @return Complete AuthResponse DTO ready for JSON serialization
     */
    public static AuthResponse toAuthResponse(String accessToken, String refreshToken, String message, User user) {
        UserInfo userInfo = toUserInfo(user); // Convert user entity to user info DTO
        AuthResponse response = new AuthResponse();
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setMessage(message);
        response.setUserInfo(userInfo);
        response.setError(null); // Explicitly set error to null for success responses
        return response;
    }

    /**
     * Creates an authentication request DTO from individual fields.
     * @param username User's username (can be null/empty for some flows)
     * @param password 
     * @param email 
     * @return AuthRequest DTO containing the provided credentials
     */
    public static AuthRequest toAuthRequest(String username, String password, String email) {
        return new AuthRequest(username, password, email);
    }
}