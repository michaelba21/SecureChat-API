package com.securechat.repository;

import com.securechat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository 
public interface UserRepository extends JpaRepository<User, UUID> {


    // Find a user by email address (used for login and email-based operations)
    Optional<User> findByEmail(String email);

    // Find a user by username (used for login and username-based operations)
    Optional<User> findByUsername(String username);

    // Search users by username with case-insensitive partial matching
    List<User> findByUsernameContainingIgnoreCase(String username);
}