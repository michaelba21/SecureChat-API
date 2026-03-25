package com.securechat.mapper;

import com.securechat.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LoginDtoMapper Tests")  // Descriptive class name for test reporting
class LoginDtoMapperTest {
    // This test class validates the LoginDtoMapper which creates LoginRequest DTOs
    

    @Test
    @DisplayName("toLoginRequest creates LoginRequest with correct email and password")
    void toLoginRequest_mapsCorrectly() {
        // Tests the normal/happy path for login request creation
        // Login typically requires email (or username) and password
        
        // Arrange - Setup test data
        String email = "alice@example.com";        
        String password = "securePassword123";     // User's password (should be hashed in production)

        // Act - Execute the mapper method
        LoginRequest request = LoginDtoMapper.toLoginRequest(email, password);

        // Assert - Verify the LoginRequest is correctly created
        assertThat(request).isNotNull();  // Should always return a LoginRequest object
        assertThat(request.getEmail()).isEqualTo(email);      
        assertThat(request.getPassword()).isEqualTo(password); // Password should match input
    }

    @Test
    @DisplayName("toLoginRequest handles null email")
    void toLoginRequest_withNullEmail() {
        // Tests edge case: email is null (might happen in some authentication flows)
        // This could represent username-based login or malformed requests
        String password = "password";

        LoginRequest request = LoginDtoMapper.toLoginRequest(null, password);

        assertThat(request).isNotNull();  // Should still create a LoginRequest
        assertThat(request.getEmail()).isNull();        
        assertThat(request.getPassword()).isEqualTo(password); // Password should still be set
    }

    @Test
    @DisplayName("toLoginRequest handles null password")
    void toLoginRequest_withNullPassword() {
        // Tests edge case: password is null
        // This could happen in password reset flows or malformed requests
        String email = "bob@example.com";

        LoginRequest request = LoginDtoMapper.toLoginRequest(email, null);

        assertThat(request).isNotNull();
        assertThat(request.getEmail()).isEqualTo(email);  
        assertThat(request.getPassword()).isNull();       // Password should be null as provided
    }

    @Test
    @DisplayName("toLoginRequest handles both null values")
    void toLoginRequest_withBothNull() {
        // Tests the extreme edge case: both parameters are null
        // This tests the mapper's robustness against completely empty requests
        LoginRequest request = LoginDtoMapper.toLoginRequest(null, null);

        assertThat(request).isNotNull();  // Should still create a LoginRequest object
        assertThat(request.getEmail()).isNull();   
        assertThat(request.getPassword()).isNull(); 

    }

    @Test
    @DisplayName("Private constructor prevents instantiation")
    void privateConstructor_cannotBeInstantiated() {
        // Tests that LoginDtoMapper follows the utility class pattern
     
        // Using reflection to attempt to instantiate the class
        assertThatThrownBy(() -> {
            java.lang.reflect.Constructor<LoginDtoMapper> constructor =
                    LoginDtoMapper.class.getDeclaredConstructor();  // Get private constructor
            constructor.setAccessible(true);  
            constructor.newInstance();        // Attempt to create instance
        })
        .isInstanceOf(java.lang.reflect.InvocationTargetException.class)  
        .hasCauseInstanceOf(IllegalStateException.class) // or IllegalAccessError depending on JVM
        // The actual exception type depends on implementation:
        //  IllegalStateException if constructor throws it explicitly

        .hasRootCauseMessage("Utility class");  // Expected error message in exception
        
    
    }
}