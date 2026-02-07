package com.securechat.config;

import com.securechat.entity.User;  
import com.securechat.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component  // Marks this class as a Spring component that will be auto-detected and managed
public class DataInitializer implements CommandLineRunner {
    // This class runs automatically when Spring Boot application starts

    private final UserRepository userRepository;  
    private final PasswordEncoder passwordEncoder;  // Spring Security component for password hashing

    // Constructor injection: Spring automatically provides these dependencies
    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // This method executes when the application starts
        
        // Check if admin user already exists in the database
        if (userRepository.findByUsername("admin").isEmpty()) {
            // Create new admin user only if it doesn't exist
            User admin = new User();  
            
            // Set basic user properties
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));  // Fixed: correct setter name (matches entity)
            admin.setEmail("admin@example.com");
            admin.setIsActive(true);  // Fixed: matches your User entity field name (isActive)
            
            // Optional: assign admin role (recommended for security)
            // Setting multiple roles using a Set
            admin.setRoles(Set.of(User.UserRole.ROLE_ADMIN, User.UserRole.ROLE_USER));
            
            // Save the admin user to the database
            userRepository.save(admin);

            // Print confirmation message to console
            System.out.println("=== CREATED TEST ADMIN USER ===");
            System.out.println("Username: admin");
            System.out.println("Password: admin123 (hashed)");  // Note: actual stored password is hashed
            System.out.println("Email   : admin@example.com");
            System.out.println("Roles   : ROLE_ADMIN, ROLE_USER");
            System.out.println("===============================");
        }
        // If admin user already exists, nothing happens - prevents duplicate creation
    }
}