package com.securechat.mapper;

import com.securechat.dto.AuthRequest;
import com.securechat.dto.AuthResponse;
import com.securechat.dto.AuthResponse.UserInfo;
import com.securechat.entity.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class AuthDtoMapperTest {
    // This test class validates the AuthDtoMapper which converts between
    // authentication-related DTOs (Data Transfer Objects) and Entity objects
    
    // Test constants for consistent test data
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USERNAME = "john_doe";
    private static final String EMAIL = "john@example.com";
    private static final String PASSWORD = "secret123";
    private static final String ACCESS_TOKEN = "jwt.access.token";
    private static final String REFRESH_TOKEN = "jwt.refresh.token";
    private static final String MESSAGE = "Login successful";

    @Test
    void toUserInfo_mapsAllFieldsCorrectly_whenUserHasId() {
        // Tests the mapping from User entity to UserInfo DTO with all fields populated
        // This is the normal/happy path scenario
        User user = new User();
        user.setId(USER_ID);        // UUID identifier
        user.setUsername(USERNAME); 
        user.setEmail(EMAIL);       // User's email address

        UserInfo userInfo = AuthDtoMapper.toUserInfo(user);

        // AssertJ fluent assertions for comprehensive validation
        assertThat(userInfo)
                .isNotNull()  // Ensures mapping doesn't return null
                .hasFieldOrPropertyWithValue("userId", USER_ID.toString())  // UUID converted to String
                .hasFieldOrPropertyWithValue("username", USERNAME)
                .hasFieldOrPropertyWithValue("email", EMAIL);
    }

    @Test
    void toUserInfo_handlesNullUserIdGracefully() {
        // Tests edge case: User entity without ID (e.g., newly created user not yet persisted)
        // Important for handling new user registration scenarios
        User user = new User();
        user.setUsername(USERNAME);
        user.setEmail(EMAIL);
        // id is null - simulating user before database persistence

        UserInfo userInfo = AuthDtoMapper.toUserInfo(user);

        assertThat(userInfo)
                .isNotNull()
                .hasFieldOrPropertyWithValue("userId", null)  // userId should be null when User.id is null
                .hasFieldOrPropertyWithValue("username", USERNAME)
                .hasFieldOrPropertyWithValue("email", EMAIL);
    }

    @Test
    void toUserInfo_returnsNull_whenUserIsNull() {
        // Tests null safety: mapper should handle null input gracefully
        // Prevents NullPointerException in calling code
        UserInfo userInfo = AuthDtoMapper.toUserInfo(null);

        assertThat(userInfo).isNull();  // Should return null when input is null
    }

    @Test
    void toAuthResponse_mapsAllFieldsCorrectly_includingUserInfo() {
        // Tests complete AuthResponse creation with all components
        // This is used for successful authentication responses (login/registration)
        User user = new User();
        user.setId(USER_ID);
        user.setUsername(USERNAME);
        user.setEmail(EMAIL);

        // Maps all authentication response components
        AuthResponse response = AuthDtoMapper.toAuthResponse(
            ACCESS_TOKEN,    // JWT access token for API authorization
            REFRESH_TOKEN,   
            MESSAGE,         // Human-readable success message
            user            
        );

        // Validate top-level AuthResponse fields
        assertThat(response)
                .isNotNull()
                .hasFieldOrPropertyWithValue("token", ACCESS_TOKEN)
                .hasFieldOrPropertyWithValue("refreshToken", REFRESH_TOKEN)
                .hasFieldOrPropertyWithValue("message", MESSAGE)
                .hasFieldOrPropertyWithValue("error", null);  // Error should be null for success responses

        // Validate nested UserInfo object within AuthResponse
        UserInfo userInfo = response.getUserInfo();
        assertThat(userInfo)
                .isNotNull()
                .hasFieldOrPropertyWithValue("userId", USER_ID.toString())
                .hasFieldOrPropertyWithValue("username", USERNAME)
                .hasFieldOrPropertyWithValue("email", EMAIL);
    }

    @Test
    void toAuthResponse_handlesNullUser_correctlySetsUserInfoToNull() {
        // Tests scenario where user info might not be needed in response
        // Could be used for token refresh endpoints or logout responses
        AuthResponse response = AuthDtoMapper.toAuthResponse(ACCESS_TOKEN, REFRESH_TOKEN, MESSAGE, null);

        assertThat(response)
                .isNotNull()
                .hasFieldOrPropertyWithValue("token", ACCESS_TOKEN)
                .hasFieldOrPropertyWithValue("refreshToken", REFRESH_TOKEN)
                .hasFieldOrPropertyWithValue("message", MESSAGE)
                .hasFieldOrPropertyWithValue("error", null)
                .hasFieldOrPropertyWithValue("userInfo", null);  // UserInfo should be null when user is null
    }

    @Test
    void toAuthRequest_createsDtoWithCorrectFields() {
        // Tests creation of AuthRequest DTO for registration scenario
        // Registration requires username, password, AND email
        AuthRequest request = AuthDtoMapper.toAuthRequest(USERNAME, PASSWORD, EMAIL);

        assertThat(request)
                .isNotNull()
                .hasFieldOrPropertyWithValue("username", USERNAME)
                .hasFieldOrPropertyWithValue("password", PASSWORD)
                .hasFieldOrPropertyWithValue("email", EMAIL);
    }

    @Test
    void toAuthRequest_worksWithNullEmail_forLoginScenario() {
        // Tests creation of AuthRequest DTO for login scenario
        // Login typically requires only username and password (email may be null)
        AuthRequest request = AuthDtoMapper.toAuthRequest(USERNAME, PASSWORD, null);

        assertThat(request)
                .isNotNull()
                .hasFieldOrPropertyWithValue("username", USERNAME)
                .hasFieldOrPropertyWithValue("password", PASSWORD)
                .hasFieldOrPropertyWithValue("email", null);  // Email can be null for login requests
    }
}