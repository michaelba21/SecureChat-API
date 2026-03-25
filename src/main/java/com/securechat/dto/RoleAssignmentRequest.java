package com.securechat.dto;

import com.securechat.entity.User;
import java.util.Set;

/**
 * DTO for role assignment requests
 * Implements FE-USER-014: Admin can assign user roles
 */
public class RoleAssignmentRequest {
    
    private Set<User.UserRole> roles;  // Set of roles to assign to a user (ensures uniqueness)

   
    public RoleAssignmentRequest() {
    }
    // Parameterized constructor with defensive copying for immutability
    public RoleAssignmentRequest(Set<User.UserRole> roles) {
        this.roles = roles != null ? Set.copyOf(roles) : null; // Defensive copy to prevent external modification
    }
    // Getter returns the internal set (caller should not modify)
    public Set<User.UserRole> getRoles() {
        return roles;
    }
    // Setter with defensive copying for immutability
    public void setRoles(Set<User.UserRole> roles) {
        this.roles = roles != null ? Set.copyOf(roles) : null; 
    }
}