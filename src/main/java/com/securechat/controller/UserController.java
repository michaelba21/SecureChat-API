package com.securechat.controller;

import com.securechat.dto.ChatRoomDTO;
import com.securechat.entity.User;
import com.securechat.exception.UnauthorizedException;
import com.securechat.service.ChatRoomService;
import com.securechat.service.UserService;
import com.securechat.service.UserSyncService;
import com.securechat.util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping({ "/users", "/api/users" })// Dual path mapping for flexibility
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private UserSyncService userSyncService;

    // Helper to get current user ID safely from Keycloak JWT
    private UUID getCurrentUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        if (!(auth instanceof JwtAuthenticationToken)) {
            throw new UnauthorizedException("Authentication is not a JWT token");
        }

        try {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
            Jwt jwt = jwtAuth.getToken();
            
            // Extract UUID from Keycloak JWT subject claim
            String keycloakUuid = jwt.getSubject();
            if (keycloakUuid == null || keycloakUuid.trim().isEmpty()) {
                throw new UnauthorizedException("JWT subject claim is null or empty");
            }
            
            UUID userId = UUID.fromString(keycloakUuid);
            logger.debug("[UserController] Authenticated Keycloak user: {}", userId);
            return userId;
            
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid user identifier from Keycloak JWT", e);
        }
    }
 // GET all users (public/authenticated endpoint)
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        try {
            return ResponseEntity.ok(userService.getAllUsers());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
 // GET specific user by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable UUID id) {
        User user = userService.getUserById(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }
// PUT full user update with Spring Security authorization
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #id == authentication.principal.id") // Safer: compare UUID directly
    public ResponseEntity<User> updateUser(@PathVariable UUID id, @RequestBody User userDetails) {
        try {
            return ResponseEntity.ok(userService.updateUser(id, userDetails));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
   // PATCH partial user update with same security rules as PU
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<User> partialUpdateUser(@PathVariable UUID id, @RequestBody Map<String, Object> updates) {
        try {
            return ResponseEntity.ok(userService.partialUpdateUser(id, updates));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
// GET users by search query
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String q) {
        return ResponseEntity.ok(userService.searchUsers(q));
    }
 // PUT update user roles - ADMIN only endpoint
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<User> updateUserRoles(@PathVariable UUID id, @RequestBody Set<User.UserRole> roles) {
        return ResponseEntity.ok(userService.updateUserRoles(id, roles));
    }
   // DELETE user - ADMIN only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
 // GET current user profile - synced with Keycloak authentication
    @GetMapping({ "/me", "/profile" })
    public ResponseEntity<User> getMe(Authentication authentication) {
        try {
            // Extract UUID from Keycloak JWT
            UUID keycloakUserId = getCurrentUserId(authentication);
            
            // Ensure user is synced with local database
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();
            User syncedUser = userSyncService.getOrCreateUser(jwt);
            
            logger.info("[UserController /me] Keycloak user: {} → Local user ID: {}, username: {}", 
                keycloakUserId, syncedUser.getId(), syncedUser.getUsername());
            
            return ResponseEntity.ok(syncedUser);
        } catch (UnauthorizedException e) {
            logger.warn("[UserController /me] Unauthorized: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            logger.error("[UserController /me] Error retrieving user profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // PUT update current user's profile
    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(Authentication authentication, @RequestBody User userDetails) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        try {
            UUID userId = getCurrentUserId(authentication);
            return ResponseEntity.ok(userService.updateUser(userId, userDetails));
        } catch (UnauthorizedException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    // GET chatrooms created by a specific user
    @GetMapping("/{id}/chatrooms/created")
    public ResponseEntity<List<ChatRoomDTO>> getCreatedChatrooms(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(chatRoomService.getChatroomsByCreator(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}