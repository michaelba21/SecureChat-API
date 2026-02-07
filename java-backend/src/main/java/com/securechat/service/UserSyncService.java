package com.securechat.service;

import com.securechat.entity.User;
import com.securechat.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service // Marks this as a Spring service bean
public class UserSyncService {

    private static final Logger logger = LoggerFactory.getLogger(UserSyncService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserSyncService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
  // Create a placeholder password hash (not used for Keycloak auth, but required for entit
    @Transactional
    public User getOrCreateUser(Jwt jwt) {
          // Extract subject (user ID) from JWT token
        String subject = jwt.getSubject();
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT subject claim is null or empty");
        }
      // Convert subject string to UUID (Keycloak user ID)
        UUID userId = UUID.fromString(subject);
        Optional<User> existing = userRepository.findById(userId);
        if (existing.isPresent()) {
            return existing.get();
        }
 // Create new user since it doesn't exist locally
        User user = new User();
        user.setId(userId);

        String resolvedUsername = resolveUsername(jwt, userId);
        String resolvedEmail = resolveEmail(jwt, userId);
  // Create a placeholder password hash (not used for Keycloak auth, but required for entit
        user.setUsername(resolvedUsername);
        user.setEmail(resolvedEmail);
        user.setPasswordHash(passwordEncoder.encode("keycloak:" + userId));
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        user.setStatus(User.UserStatus.OFFLINE);
    // Set user roles based on JWT claims
        Set<User.UserRole> roles = new HashSet<>();
        roles.add(User.UserRole.ROLE_USER);
           // Add ADMIN role if JWT indicates admin privileges
        if (hasAdminRole(jwt)) {
            roles.add(User.UserRole.ROLE_ADMIN);
        }
        user.setRoles(roles);
   // Save new user to database
        User saved = userRepository.save(user);
        logger.info("Provisioned local user for Keycloak subject: {}", userId);
        return saved;
    }
/**
     * Extracts username from JWT claims with fallback strategies
     */
    private String resolveUsername(Jwt jwt, UUID userId) {
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            username = jwt.getClaimAsString("email");
        }
        if (username == null || username.isBlank()) {
            username = "user-" + userId;
        }

        return ensureUniqueUsername(username, userId);
    }
 /**
     * Extracts email from JWT claims with fallback
     */
    private String resolveEmail(Jwt jwt, UUID userId) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = userId + "@keycloak.local";
        }

        return ensureUniqueEmail(email, userId);
    }
/**
     * Ensures username uniqueness: if duplicate exists, generates unique one
     */
    private String ensureUniqueUsername(String username, UUID userId) {
        return userRepository.findByUsername(username)
            .filter(existing -> !existing.getId().equals(userId))
            .map(existing -> "user-" + userId)
            .orElse(username);
    }
/**
     * Ensures email uniqueness: if duplicate exists, generates unique one
     */
    private String ensureUniqueEmail(String email, UUID userId) {
        return userRepository.findByEmail(email)
            .filter(existing -> !existing.getId().equals(userId))
            .map(existing -> userId + "@keycloak.local")
            .orElse(email);
    }
/**
     * Checks if user has admin role in JWT claims
     */
    private boolean hasAdminRole(Jwt jwt) {
        if (hasRole(jwt, "admin") || hasRole(jwt, "ROLE_ADMIN") || hasRole(jwt, "ADMIN")) {
            return true;
        }

        return false;
    }
/**
     * Generic role checker: looks for role in both realm_access and resource_access claims
     */
    @SuppressWarnings("unchecked")
    private boolean hasRole(Jwt jwt, String role) {
          // Check realm_access.roles (Keycloak realm-level roles)
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
            if (roles.stream().anyMatch(r -> role.equalsIgnoreCase(String.valueOf(r)))) {
                return true;
            }
        }
 // Check resource_access.client.roles (Keycloak client-specific roles)
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            Object clientAccess = resourceAccess.get("securechat-backend");
            if (clientAccess instanceof Map<?, ?> clientMap) {
                Object clientRoles = clientMap.get("roles");
                if (clientRoles instanceof List<?> roles) {
                    return roles.stream().anyMatch(r -> role.equalsIgnoreCase(String.valueOf(r)));
                }
            }
        }

        return false;
    }
}
