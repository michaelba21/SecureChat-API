package com.securechat.config;
import com.securechat.entity.User;
import com.securechat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired/// Inject UserRepository dependency for data initialization
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder; // BCrypt for secure password hashing

    @Override
    public void run(String... args) {
        // Create default admin user if not exists and Check if admin user already exists
        
        if (userRepository.findByUsername("admin").isEmpty()) { // Check if admin exists
            // Create new admin user
            User admin = new User();
            admin.setUsername("admin"); 
            admin.setEmail("admin@securechat.com"); // Set admin email
            admin.setPasswordHash(passwordEncoder.encode("admin123"));// Securely hash password
            admin.setCreatedAt(LocalDateTime.now()); 
            admin.setIsActive(true); // Activate admin account
            admin.getRoles().add(User.UserRole.ROLE_ADMIN);
            userRepository.save(admin); // Persist admin to database
            System.out.println("Created default admin user: admin@securechat.com / admin123"); // Log creation
        }
    }
}