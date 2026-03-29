package com.securechat.controller;

import com.securechat.entity.User;
import com.securechat.repository.UserRepository;
import com.securechat.repository.ChatRoomMemberRepository;
import com.securechat.service.UserSyncService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.UUID;

/**
 * Authentication Status & Verification Controller
 * 
 * Provides endpoints to verify Keycloak authentication is working correctly.
 * These endpoints are crucial for debugging 401/403 errors.
 */
@RestController
@RequestMapping("/api/auth")
@PreAuthorize("isAuthenticated()") // All endpoints require authentication
@RequiredArgsConstructor
public class AuthStatusController {

    private static final Logger logger = LoggerFactory.getLogger(AuthStatusController.class);

    private final UserSyncService userSyncService;
    private final UserRepository userRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    /**
     * Get current authenticated user info from Keycloak JWT.
     * This is THE critical endpoint for verifying authentication works.
     */
    @GetMapping("/whoami")
    public ResponseEntity<Map<String, Object>> whoAmI(Jwt jwt) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");

        // Sync/create user in local database if doesn't exist
        User localUser = userSyncService.getOrCreateUser(jwt);

        logger.info("User {} authenticated from Keycloak", keycloakId);

        return ResponseEntity.ok(Map.ofEntries(
                Map.entry("keycloakId", keycloakId.toString()),
                Map.entry("localDatabaseId", localUser.getId().toString()),
                Map.entry("username", username),
                Map.entry("email", email),
                Map.entry("localUserExists", true),
                Map.entry("localUsername", localUser.getUsername()),
                Map.entry("message", " Authentication successful - User synced to database")));
    }

    /**
     * Verify user exists in local database (post-sync).
     * Debugging endpoint to confirm UserSyncService created the user correctly.
     */
    @GetMapping("/verify-user")
    public ResponseEntity<Map<String, Object>> verifyUser(Jwt jwt) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());

        Optional<User> localUser = userRepository.findById(keycloakId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("keycloakId", keycloakId.toString());
        response.put("foundInDatabase", localUser.isPresent());

        if (localUser.isPresent()) {
            User user = localUser.get();
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("isActive", user.getIsActive());
            response.put("status", " User found in database");
        } else {
            response.put("status", " User NOT in database - sync may have failed");
            response.put("troubleshooting", List.of(
                    "1. Check UserSyncService logs",
                    "2. Verify Keycloak user exists in realm",
                    "3. Restart application to retry sync"));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Verify chat room membership after authentication.
     * Critical for debugging 403 Forbidden errors on chatroom endpoints.
     */
    @GetMapping("/verify-membership/{chatRoomId}")
    public ResponseEntity<Map<String, Object>> verifyMembership(
            Jwt jwt,
            @PathVariable UUID chatRoomId) {

        UUID keycloakId = UUID.fromString(jwt.getSubject());

        boolean isMember = chatRoomMemberRepository
                .existsByChatRoomIdAndUserId(chatRoomId, keycloakId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("keycloakUserId", keycloakId.toString());
        response.put("chatRoomId", chatRoomId.toString());
        response.put("isMember", isMember);
        response.put("status", isMember ? " User IS a member" : "❌ User NOT a member");

        if (!isMember) {
            response.put("possibleCauses", List.of(
                    "1. User not added to chat room",
                    "2. User was removed from chat room",
                    "3. Chat room memberships use different UUID (migration needed)"));
            response.put("solution", "Add user to chat room via POST /api/chatrooms/{id}/members");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "Keycloak authentication is working"));
    }
}
