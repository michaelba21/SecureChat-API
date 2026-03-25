package com.securechat.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;
@Data 
@ToString(exclude = "password") // Security: Excludes password from toString() method
public class RegisterRequest {
    @NotBlank // Validation: Requires non-null and non-empty string (trimmed)
    private String username;  

    @NotBlank 
    private String password;  

    @Email // Validation: Ensures field contains valid email format
    @NotBlank 
    private String email;  // User's email address for account identification

  
    public RegisterRequest() {}

    // Parameterized constructor for manual object creation
    public RegisterRequest(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
    // Explicit getters and setters (provided alongside Lombok @Data for clarity)
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}