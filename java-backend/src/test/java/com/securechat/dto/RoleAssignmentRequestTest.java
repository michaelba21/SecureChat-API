package com.securechat.dto;

import com.securechat.entity.User;  // User entity for UserRole enum
import org.junit.jupiter.api.Test;

import java.util.HashSet;  // Set implementation
import java.util.Set;  

import static org.assertj.core.api.Assertions.*;  

class RoleAssignmentRequestTest {

    @Test
    void defaultConstructor_createsEmptyObject() {
        // Test: Default constructor creates object with null roles field
        RoleAssignmentRequest request = new RoleAssignmentRequest();  

        assertThat(request.getRoles()).isNull();  // Roles should be null by default
    }

    @Test
    void parameterizedConstructor_setsRolesCorrectly() {
        // Test: Constructor with roles parameter correctly sets the roles
        Set<User.UserRole> roles = Set.of(User.UserRole.ROLE_ADMIN, User.UserRole.ROLE_USER);  // Create immutable set with two roles

        RoleAssignmentRequest request = new RoleAssignmentRequest(roles);  

        assertThat(request.getRoles())  // Verify roles field
                .isNotNull()  
                .hasSize(2)  // Should contain exactly 2 roles
                .containsExactlyInAnyOrder(User.UserRole.ROLE_ADMIN, User.UserRole.ROLE_USER);  // Should contain both roles in any order
    }

    @Test
    void parameterizedConstructor_withNullRoles_setsNull() {
        // Test: Constructor accepts null roles parameter
        RoleAssignmentRequest request = new RoleAssignmentRequest(null);  

        assertThat(request.getRoles()).isNull();  // Roles should be null
    }

    @Test
    void setter_andGetter_workCorrectly() {
        // Test: Setter sets value and getter retrieves it correctly
        RoleAssignmentRequest request = new RoleAssignmentRequest();  // Create empty object

        Set<User.UserRole> roles = new HashSet<>();  // Create mutable set
        roles.add(User.UserRole.ROLE_ADMIN);  // Add ADMIN role

        request.setRoles(roles);  // Set roles using setter

        assertThat(request.getRoles())  // Verify using getter
                .isNotNull()  
                .hasSize(1)  // Should contain 1 role
                .contains(User.UserRole.ROLE_ADMIN);  
    }

    @Test
    void setter_acceptsNull() {
        // Test: Setter accepts null value (important for clearing roles)
        RoleAssignmentRequest request = new RoleAssignmentRequest();  // Create object
        request.setRoles(Set.of(User.UserRole.ROLE_USER));  

        request.setRoles(null);  // Set roles to null

        assertThat(request.getRoles()).isNull();  // Should be null after setting null
    }

    @Test
    void roles_isIndependent_afterSetting() {
        // Test: Ensures defensive copying or immutability - modifying original set doesn't affect DTO
        Set<User.UserRole> originalRoles = new HashSet<>();  
        originalRoles.add(User.UserRole.ROLE_USER);  // Add USER role

        RoleAssignmentRequest request = new RoleAssignmentRequest();  // Create DTO
        request.setRoles(originalRoles);  // Set roles from original set

        // Modify the original set after it's been set in the DTO
        originalRoles.add(User.UserRole.ROLE_ADMIN);  

        // The DTO's roles should remain unchanged (defensive copy or immutable)
        assertThat(request.getRoles())  
                .hasSize(1)  // Should still have only 1 role (not 2)
                .containsOnly(User.UserRole.ROLE_USER); 
    }

    @Test
    void fullObject_canBeConstructedAndReadCorrectly() {
        // Test: Full object construction and reading
        Set<User.UserRole> roles = Set.of(User.UserRole.ROLE_ADMIN);  // Create set with ADMIN role

        RoleAssignmentRequest request = new RoleAssignmentRequest(roles);  // Create with constructor

        assertThat(request.getRoles()) 
                .isNotNull()  // Should not be null
                .containsExactly(User.UserRole.ROLE_ADMIN);  // Should contain exactly ADMIN role
    }
}