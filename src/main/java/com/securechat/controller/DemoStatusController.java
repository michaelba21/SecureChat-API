package com.securechat.controller;

import com.securechat.entity.User;
import com.securechat.entity.ChatRoom;
import com.securechat.entity.ChatRoomMember;
import com.securechat.repository.UserRepository;
import com.securechat.repository.ChatRoomRepository;
import com.securechat.repository.ChatRoomMemberRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Demo Status Endpoint - Shows examiner authentication state and database
 * consistency
 * Useful for verifying the Keycloak-only fix is working correctly
 */
@RestController
@RequestMapping("/api/demo")
public class DemoStatusController {

    private static final Logger logger = LoggerFactory.getLogger(DemoStatusController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Value("${spring.application.name:SecureChat API}")
    private String applicationName;

    /**
     * GET /api/demo/status
     * Shows current authentication state and UUID consistency check
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DemoStatus> getStatus(Authentication authentication) {
        try {
            // Extract Keycloak UUID from JWT
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();
            String keycloakUuidStr = jwt.getSubject();
            UUID keycloakUserId = UUID.fromString(keycloakUuidStr);

            DemoStatus status = new DemoStatus();
            status.setAuthenticated(true);
            status.setKeycloakUserId(keycloakUserId);
            status.setUsername(jwt.getClaimAsString("preferred_username"));
            status.setEmail(jwt.getClaimAsString("email"));
            status.setMode("Keycloak-only");
            status.setAuthProvider("Keycloak (OAuth2)");
            status.setApplicationName(applicationName);

            // Check if user exists in local database
            Optional<User> localUser = userRepository.findById(keycloakUserId);
            if (localUser.isPresent()) {
                User user = localUser.get();
                status.setLocalUserId(user.getId());
                status.setUserInDatabase(true);
                status.setUuidConsistent(user.getId().equals(keycloakUserId));

                // Get user's statistics
                List<ChatRoom> createdRooms = chatRoomRepository.findByCreatedBy_Id(keycloakUserId);
                status.setCreatedChatrooms(createdRooms.size());

                List<ChatRoomMember> memberships = chatRoomMemberRepository.findByUserId(keycloakUserId);
                status.setMemberInChatrooms(memberships.size());

                logger.info("[DEMO] User {} status: Keycloak UUID={}, Local UUID={}, consistency={}",
                        status.getUsername(), keycloakUserId, user.getId(), status.isUuidConsistent());
            } else {
                status.setUserInDatabase(false);
                status.setUuidConsistent(false);
                logger.warn("[DEMO] User {} from Keycloak (UUID={}) not found in local database",
                        status.getUsername(), keycloakUserId);
            }

            status.setTimestamp(LocalDateTime.now());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("[DEMO] Error getting status", e);
            DemoStatus errorStatus = new DemoStatus();
            errorStatus.setAuthenticated(false);
            errorStatus.setTimestamp(LocalDateTime.now());
            return ResponseEntity.ok(errorStatus);
        }
    }

    /**
     * Simple DTO to show demo status
     */
    @Data
    @AllArgsConstructor
    public static class DemoStatus {
        private boolean authenticated;
        private UUID keycloakUserId;
        private UUID localUserId;
        private String username;
        private String email;
        private boolean userInDatabase;
        private boolean uuidConsistent;
        private int createdChatrooms;
        private int memberInChatrooms;
        private String mode;
        private String authProvider;
        private String applicationName;
        private LocalDateTime timestamp;

        // Default constructor for error cases
        public DemoStatus() {
            this.authenticated = false;
            this.mode = "Keycloak-only";
            this.authProvider = "Keycloak (OAuth2)";
            this.timestamp = LocalDateTime.now();
        }
    }
}
