package com.securechat.dto;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data // Lombok: generates getters, setters, equals, hashCode, toString
@NoArgsConstructor 
@AllArgsConstructor // Lombok: generates constructor with all fields
public class AuthResponse {
    private String token; 
    private String refreshToken; // Refresh token for obtaining new access tokens
    private String message;
    private UserInfo userInfo;
    private String error; 

    // I made a constructor that matches the AuthService call (6 parameters now)
    public AuthResponse(String token, String refreshToken, String message, String username, String email,
            String userId) {
        this.token = token; // Set access token
        this.refreshToken = refreshToken; 
        this.message = message; 
        this.userInfo = new UserInfo(userId, username, email); // Create user info object
        this.error = null; // No error for successful response
    }

    // Constructor with token, userInfo, and error
    public AuthResponse(String token, UserInfo userInfo, String error) {
        this.token = token; // Set access token
        this.userInfo = userInfo; 
        this.error = error; 
        this.refreshToken = null; 
        this.message = null; // No message
    }

    // Static factory methods
    public static AuthResponse success(String token, String refreshToken, String message, String username, String email,
            String userId) {
        return new AuthResponse(token, refreshToken, message, username, email, userId); // Success response with all parameters
    }

    public static AuthResponse success(String token, UserInfo userInfo) {
        return new AuthResponse(token, userInfo, null); // Success response with token and user info
    }

    public static AuthResponse error(String errorMessage) {
        AuthResponse response = new AuthResponse(); // Create new instance
        response.setError(errorMessage); 
        return response; // Return error response
    }

    // UserInfo inner class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String userId; 
        private String username; // Display name
        private String email; 

        public String getUserId() { // Getter for userId
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUsername() { // Getter for username
            return username;
        }

        public void setUsername(String username) { 
            this.username = username;
        }

        public String getEmail() { 
            return email;
        }

        public void setEmail(String email) { 
            this.email = email;
        }
    }
}