package com.securechat.service;
import com.securechat.entity.User;
import com.securechat.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.UUID;

@Service
@Validated  // Enables method-level validation with @NotNull annotations
public class UserService {

  @Autowired
  protected UserRepository userRepository;  // Data access layer for User entities

  // Retrieve all users from the database
  @NotNull
  public List<User> getAllUsers() {
    return userRepository.findAll();  // Returns empty list if no users exist
  }

  // Retrieve a single user by their unique ID
  @NotNull
  public User getUserById(@NotNull UUID id) {
    if (id == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
    return userRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("User not found"));  // Throws if user doesn't exist
  }

  // Full update of a user - replaces all updatable fields with provided values
  @NotNull
  @SuppressWarnings("null")  // Suppresses null warnings for the method (from @NotNull return)
  public User updateUser(@NotNull UUID id, @NotNull User userDetails) {
    // Validate input parameters
    if (id == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
    if (userDetails == null) {
      throw new IllegalArgumentException("User details cannot be null");
    }
    
    // Retrieve existing user from database
    User user = getUserById(id);
    
    // Conditional field updates - only update if new value is not null
    if (userDetails.getUsername() != null) {
      user.setUsername(userDetails.getUsername());
    }
    if (userDetails.getEmail() != null) {
      user.setEmail(userDetails.getEmail());
    }
    if (userDetails.getBio() != null) {
      user.setBio(userDetails.getBio());
    }
    if (userDetails.getAvatarUrl() != null) {
      user.setAvatarUrl(userDetails.getAvatarUrl());
    }
    
    // Persist changes and return updated user
    return userRepository.save(user);
  }

  // Search users by username (case-insensitive partial match)
  @NotNull
  public List<User> searchUsers(@NotNull String query) {
    if (query == null) {
      throw new IllegalArgumentException("Search query cannot be null");
    }
    // Performance/security: prevent overly broad searches with short queries
    if (query.length() < 3) {
      throw new RuntimeException("Search query must be at least 3 characters");
    }
    return userRepository.findByUsernameContainingIgnoreCase(query);  // Returns empty list if no matches
  }

  // Partial update using a Map - allows updating specific fields without sending entire user object
  @NotNull
  public User partialUpdateUser(@NotNull UUID id, @NotNull java.util.Map<String, Object> updates) {
    if (id == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
    if (updates == null || updates.isEmpty()) {
      throw new IllegalArgumentException("Updates cannot be null or empty");
    }
    
    // Retrieve existing user
    User user = getUserById(id);
    
    // Update only the fields present in the map
    if (updates.containsKey("username")) {
      user.setUsername((String) updates.get("username"));  // Type casting - assumes String
    }
    if (updates.containsKey("email")) {
      user.setEmail((String) updates.get("email"));
    }
    if (updates.containsKey("bio")) {
      user.setBio((String) updates.get("bio"));
    }
    if (updates.containsKey("avatarUrl")) {
      user.setAvatarUrl((String) updates.get("avatarUrl"));
    }
    
    return userRepository.save(user);
  }

  // Update user roles - replaces entire roles set (used for admin role management)
  @NotNull
  public User updateUserRoles(@NotNull UUID id, @NotNull java.util.Set<User.UserRole> roles) {
    if (id == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
    if (roles == null || roles.isEmpty()) {
      throw new IllegalArgumentException("Roles cannot be null or empty");
    }
    
    User user = getUserById(id);
    user.setRoles(roles);  // Replace all existing roles with new set
    return userRepository.save(user);
  }

  // Soft delete - marks user as inactive instead of physical deletion
  public void deleteUser(UUID id) {
    User user = getUserById(id);
    user.setIsActive(false);  // Soft delete flag
    userRepository.save(user);  // Persist inactive status
  }
}