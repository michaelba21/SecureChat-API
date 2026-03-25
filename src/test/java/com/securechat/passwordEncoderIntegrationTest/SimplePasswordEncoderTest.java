
package com.securechat.passwordEncoderIntegrationTest;  

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

public class SimplePasswordEncoderTest {  // Simple test class to verify password encoding works correctly
    
    @Test
    void testBcryptPasswordEncoderDirectly() {
        // This test validates that Spring Security's BCrypt password encoder
    
        
        // Create the SAME encoder as in SecurityConfig
        // This ensures test consistency with production configuration
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new BCryptPasswordEncoder(12));  // Cost factor 12 (high security)
        
        // DelegatingPasswordEncoder allows supporting multiple hash algorithms
        PasswordEncoder encoder = new DelegatingPasswordEncoder("bcrypt", encoders);
        
        // Test password (should follow password policy in production)
        String rawPassword = "TestPassword123!";
        
        // Encode the password - generates a secure one-way hash
        String encoded = encoder.encode(rawPassword);
        
        // Debug output to console (helpful for manual verification)
        System.out.println("=== BCrypt Test Results ===");
        System.out.println("Raw password: " + rawPassword); 
        System.out.println("Encoded hash: " + encoded);      // Hash is safe to log
        System.out.println("Starts with {bcrypt}? " + encoded.startsWith("{bcrypt}"));
        System.out.println("Hash length: " + encoded.length());  // BCrypt hashes are ~60 chars
        
        // Verification assertions
        // 1. Verify hash uses correct algorithm prefix
        assertThat(encoded).startsWith("{bcrypt}");
        // 2. Verify original password matches the hash (authentication scenario)
        assertThat(encoder.matches(rawPassword, encoded)).isTrue();
        // 3. Verify wrong password does NOT match (security check)
        assertThat(encoder.matches("WrongPassword", encoded)).isFalse();
        
        System.out.println(" BCrypt with cost factor 12 is working!");
        // Cost factor 12 means 2^12 iterations (4096) - balances security vs performance
    }
}