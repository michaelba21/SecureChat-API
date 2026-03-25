package com.securechat.config;

import com.securechat.entity.User;
import com.securechat.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)  // Enables Mockito for JUnit 5
class DataInitializerTest {

    @Mock  // Creates a mock instance of UserRepository
    private UserRepository userRepository;

    @Mock  // Creates a mock instance of password encoder
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks  // Injects mocks into DataInitializer instance
    private DataInitializer dataInitializer;

    @Test
    void run_createsAdminUser_whenNotExists() throws Exception {
        // Given: admin user does not exist
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin123")).thenReturn("encoded-hash-123");

        // When: execute the initialization method
        dataInitializer.run();

        // Then: verify that save is called with correct user
        verify(userRepository).save(any(User.class));
        
        // Verify the specific interactions with detailed assertions
        verify(userRepository, times(1)).findByUsername("admin");
        verify(userRepository, times(1)).save(argThat(user -> {
            if (user == null) return false;
            // Assert all expected admin user properties are set correctly
            return "admin".equals(user.getUsername()) &&
                   "encoded-hash-123".equals(user.getPasswordHash()) &&
                   "admin@securechat.com".equals(user.getEmail()) &&
                   Boolean.TRUE.equals(user.getIsActive()) &&
                   user.getRoles() != null &&
                   user.getRoles().contains(User.UserRole.ROLE_ADMIN);
        }));
    }

    @Test
    void run_doesNotCreateUser_whenAdminAlreadyExists() throws Exception {
        // Given: admin already exists in database
        User existingAdmin = new User();
        existingAdmin.setUsername("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingAdmin));

        // When: execute the initialization method
        dataInitializer.run();

        // Then: verify save is never called (idempotent behavior)
        verify(userRepository, never()).save(any(User.class));
    }
}