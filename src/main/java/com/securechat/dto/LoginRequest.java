package com.securechat.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;
@Data 
public class LoginRequest {
    @Email // Validation: Ensures field contains valid email format
    @NotBlank // Validation: Requires non-null and non-empty string (trimmed)
    private String email;  

    @NotBlank 
    @ToString.Exclude // Security: Excludes password from toString() to prevent logging
    private String password;  // User's password (excluded from logging for security)
    
    
    public LoginRequest() {} 
    // Parameterized constructor for manual object creation
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
    // Explicit getters and setters (though @Data generates them, shown for clarity)
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
