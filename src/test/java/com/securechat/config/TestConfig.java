package com.securechat.config;

import org.springframework.boot.test.context.TestConfiguration;  // Annotation for test-specific configuration
import org.springframework.context.annotation.Bean;  
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; 
import org.springframework.security.crypto.password.PasswordEncoder;  // PasswordEncoder interface

@TestConfiguration  // Marks this as a test configuration (not loaded in production)
public class TestConfig {
    
    @Bean  // Defines a Spring bean for PasswordEncoder
    public PasswordEncoder passwordEncoder() {
        // Creates and returns a BCryptPasswordEncoder instance
        return new BCryptPasswordEncoder();
    }
}