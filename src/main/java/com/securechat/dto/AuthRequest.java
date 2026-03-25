package com.securechat.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data // Lombok annotation for getters, setters, equals, hashCode, toString
public class AuthRequest {
    @NotBlank(message = "Username is required") // Must not be null or empty
    @jakarta.validation.constraints.Size(min = 3, max = 50, message = "Username must be 3-50 characters") // Length validation
    @jakarta.validation.constraints.Pattern(
        regexp = "^[a-zA-Z0-9_]+$", // Alphanumeric and underscore only
        message = "Username can only contain letters, numbers, and underscores"
    )
    private String username; // Username for authentication
    
    @NotBlank(message = "Password is required") // Must not be null or empty
    @jakarta.validation.constraints.Pattern(
        regexp = "^(?=.*[0-9])(?=.*[A-Z]).{8,}$", // At least 8 chars, 1 digit, 1 uppercase
        message = "Password must be at least 8 characters with 1 digit and 1 uppercase letter"
    )
    private String password; // Password for authentication
    
    @jakarta.validation.constraints.Email(message = "Invalid email format") // Email format validation
    private String email; // For registration (optional for login)

    public AuthRequest() {} 

    public AuthRequest(String username, String password) { // Constructor for login
        this.username = username;
        this.password = password;
    }

    public AuthRequest(String username, String password, String email) { // Constructor for registration
        this.username = username;
        this.password = password;
        this.email = email;
    }
    
    public String getUsername() { return username; } // Getter for username
    public void setUsername(String username) { this.username = username; } // Setter for username

    public String getPassword() { return password; } 
    public void setPassword(String password) { this.password = password; } 

    public String getEmail() { return email; } 
    public void setEmail(String email) { this.email = email; } 
}