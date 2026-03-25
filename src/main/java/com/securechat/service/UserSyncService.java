package com.securechat.service;

import com.securechat.entity.User;
import com.securechat.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;

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

    /**
     * Get or create user from JWT token with proper race condition handling.
     * Uses the JWT subject (Keycloak user UUID) as the primary user ID.
     */
    @Transactional(value = jakarta.transaction.Transactional.TxType.REQUIRED)
    public User getOrCreateUser(Jwt jwt) {
        // Extract subject (Keycloak user UUID) from JWT token
        String keycloakSub = jwt.getSubject();
        if (keycloakSub == null || keycloakSub.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT subject claim is null or empty");
        }

        // Convert Keycloak sub to UUID for use as user ID
        UUID userId;
        try {
            userId = UUID.fromString(keycloakSub);
        } catch (IllegalArgumentException e) {
            // If Keycloak sub is not a valid UUID, generate one
            userId = UUID.randomUUID();
        }

        // Fast-path: Try to find existing user by ID first
        Optional<User> existing = userRepository.findById(userId);
        if (existing.isPresent()) {
            logger.debug("User already exists for Keycloak sub: {}", keycloakSub);
            return existing.get();
        }

        // Extract email from JWT claims
        String emailClaim = jwt.getClaimAsString("email");
        
        // CRITICAL: Check by real email if provided BEFORE creating new user
        // This prevents duplicate users when same email is used with different Keycloak accounts
        if (emailClaim != null && !emailClaim.isBlank()) {
            logger.debug("Checking for existing user with email: {}", emailClaim);
            Optional<User> existingByEmail = userRepository.findByEmail(emailClaim);
            if (existingByEmail.isPresent()) {
                logger.info("Found existing user by email: {} (sub: {})", emailClaim, keycloakSub);
                return existingByEmail.get();
            }
        }

        // Extract preferred username from JWT
        String preferredUsername = jwt.getClaimAsString("preferred_username");

        // Prepare username and email
        // Use real Keycloak email if provided, otherwise generate unique synthetic email
        String email = (emailClaim != null && !emailClaim.isBlank()) 
            ? emailClaim 
            : generateUniqueEmail(userId);

        // Use preferred_username from JWT if available and unique, otherwise generate unique username
        String username;
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            // Check if preferred username is already taken
            Optional<User> existingByUsername = userRepository.findByUsername(preferredUsername);
            if (existingByUsername.isPresent()) {
                // Preferred username taken, generate unique variant
                logger.info("Preferred username '{}' already taken, generating unique variant", preferredUsername);
                username = generateUniqueUsernameWithPrefix(preferredUsername, userId);
            } else {
                username = preferredUsername;
            }
        } else {
            username = generateUniqueUsername(userId);
        }

        // Create new user
        User newUser = new User();
        newUser.setId(userId);
        newUser.setEmail(email);
        newUser.setUsername(username);
        newUser.setPasswordHash(passwordEncoder.encode("keycloak:" + keycloakSub));
        newUser.setIsActive(true);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setLastLogin(LocalDateTime.now());
        newUser.setStatus(User.UserStatus.OFFLINE);

        // Set user roles based on JWT claims
        Set<User.UserRole> roles = new HashSet<>();
        roles.add(User.UserRole.ROLE_USER);

        // Add ADMIN role if JWT indicates admin privileges
        if (hasAdminRole(jwt)) {
            roles.add(User.UserRole.ROLE_ADMIN);
        }
        newUser.setRoles(roles);

        try {
            // Save new user
            User saved = userRepository.save(newUser);
            logger.info("Provisioned local user for Keycloak sub: {}", keycloakSub);
            return saved;

        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread/instance inserted at same time
            logger.warn("Race condition detected during user creation for sub {}, attempting recovery: {}", keycloakSub, e.getMessage());

            // Recovery attempt 1: lookup by ID
            Optional<User> recovered = userRepository.findById(userId);
            if (recovered.isPresent()) {
                logger.info("Successfully recovered user after race condition by id: {}", userId);
                return recovered.get();
            }

            // Recovery attempt 2: lookup by username
            recovered = userRepository.findByUsername(username);
            if (recovered.isPresent()) {
                logger.info("Recovered user by username after race condition: {}", username);
                return recovered.get();
            }

            // Recovery attempt 3: lookup by email
            recovered = userRepository.findByEmail(email);
            if (recovered.isPresent()) {
                logger.info("Recovered user by email after race condition: {}", email);
                return recovered.get();
            }

            // Unable to recover
            logger.error("Failed to recover user after race condition for sub {}", keycloakSub);
            throw new RuntimeException("Cannot recover user after race condition: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a guaranteed unique username using loop-based verification.
     * This handles race conditions by continuously checking uniqueness until success.
     */
    private String generateUniqueUsername(UUID userId) {
        String baseUsername = "user-" + userId;
        int attempt = 0;
        String username = baseUsername;

        // Try base username first, then numbered variants if conflicts exist
        while (userRepository.findByUsername(username).isPresent()) {
            attempt++;
            username = baseUsername + "-" + attempt;
            
            // Safety limit to prevent infinite loops
            if (attempt > 1000) {
                logger.error("Unable to generate unique username after 1000 attempts for userId: {}", userId);
                throw new RuntimeException("Cannot generate unique username for user: " + userId);
            }
        }

        logger.debug("Generated unique username for userId {}: {}", userId, username);
        return username;
    }

    /**
     * Generates a unique username based on a preferred prefix.
     * If the preferred username is taken, appends a number to make it unique.
     */
    private String generateUniqueUsernameWithPrefix(String preferredPrefix, UUID userId) {
        String baseUsername = preferredPrefix;
        int attempt = 0;
        String username = baseUsername;

        // Try base username first, then numbered variants if conflicts exist
        while (userRepository.findByUsername(username).isPresent()) {
            attempt++;
            username = baseUsername + "-" + attempt;
            
            // Safety limit to prevent infinite loops
            if (attempt > 1000) {
                logger.error("Unable to generate unique username from prefix '{}' after 1000 attempts", preferredPrefix);
                // Fallback to UUID-based username
                return generateUniqueUsername(userId);
            }
        }

        logger.debug("Generated unique username from prefix '{}': {}", preferredPrefix, username);
        return username;
    }

    /**
     * Generates a guaranteed unique email address using loop-based verification.
     * This handles race conditions by continuously checking uniqueness until success.
     */
    private String generateUniqueEmail(UUID userId) {
        String baseEmail = userId + "@keycloak.local";
        int attempt = 0;
        String email = baseEmail;

        // Try base email first, then numbered variants if conflicts exist
        while (userRepository.findByEmail(email).isPresent()) {
            attempt++;
            email = userId + "+" + attempt + "@keycloak.local";

            // Safety limit to prevent infinite loops
            if (attempt > 1000) {
                logger.error("Unable to generate unique email after 1000 attempts for userId: {}", userId);
                throw new RuntimeException("Cannot generate unique email for user: " + userId);
            }
        }

        logger.debug("Generated unique email for userId {}: {}", userId, email);
        return email;
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
