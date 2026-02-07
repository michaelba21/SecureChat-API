package com.securechat.controller;

import com.securechat.dto.ChatRoomDTO;
import com.securechat.entity.User;
import com.securechat.exception.UnauthorizedException;
import com.securechat.service.ChatRoomService;
import com.securechat.service.UserService;
import com.securechat.util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private AuthUtil authUtil;

    // Helper to get current user ID safely
    private UUID getCurrentUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        try {
            return authUtil.getCurrentUserId(auth);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid user identifier", e);
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
 // GET current user profile - uses authentication to identify user
    @GetMapping({ "/me", "/profile" })
    public ResponseEntity<User> getMe(Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("Authenticated user not found");
        }
        return ResponseEntity.ok(user);
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