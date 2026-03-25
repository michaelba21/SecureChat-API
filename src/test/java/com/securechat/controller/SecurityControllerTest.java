

package com.securechat.controller;

import com.securechat.config.TestSecurityConfig; // Custom test security configuration
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest; // MVC test slice
import org.springframework.context.annotation.Import; // Import configuration
import org.springframework.security.test.context.support.WithAnonymousUser; // Mock anonymous user
import org.springframework.security.test.context.support.WithMockUser; // Mock authenticated user
import org.springframework.test.web.servlet.MockMvc; // MVC testing

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityController.class) 
@Import(TestSecurityConfig.class) // Import custom test security configuration
@DisplayName("SecurityController → 100% coverage tests") // Test class description
class SecurityControllerTest {

    @Autowired
    private MockMvc mockMvc; // MVC test client

    // ────────────────────────────────────────────────
    //  GET /api/security/public - Public endpoint tests
    // ────────────────────────────────────────────────

    @Nested // Group tests for public endpoint
    @DisplayName("GET /api/security/public")
    class PublicEndpoint {

        @Test
        @DisplayName("Anyone can access → returns correct message")
        @WithAnonymousUser // Mock anonymous user (no authentication)
        void publicEndpoint_shouldBeAccessibleWithoutAuthentication() throws Exception {
            // Act & Assert: public endpoint should be accessible without auth
            mockMvc.perform(get("/api/security/public")) // GET request
                   .andExpect(status().isOk()) 
                   .andExpect(content().string("Dit is een publiek endpoint")); // Dutch: "This is a public endpoint"
        }

        @Test
        @DisplayName("Authenticated user can also access")
        @WithMockUser(username = "alice") // Mock authenticated user
        void publicEndpoint_authenticatedUser_shouldSucceed() throws Exception {
            // Act & Assert: public endpoint should also work for authenticated users
            mockMvc.perform(get("/api/security/public"))
                   .andExpect(status().isOk()) // HTTP 200 OK
                   .andExpect(content().string("Dit is een publiek endpoint")); // Same message
        }
    }

    // ────────────────────────────────────────────────
    //  GET /api/security/private - Private endpoint tests
    // ────────────────────────────────────────────────

    @Nested // Group tests for private endpoint
    @DisplayName("GET /api/security/private")
    class PrivateEndpoint {

        @Test
        @DisplayName("Anonymous user → should be denied (401/403)")
        @WithAnonymousUser // Mock anonymous user
        void privateEndpoint_anonymous_shouldBeForbiddenOrUnauthorized() throws Exception {
            // Act & Assert: anonymous users should be denied access
            mockMvc.perform(get("/api/security/private")) // GET request
                   .andExpect(status().is4xxClientError()); // Either 401 Unauthorized or 403 Forbidden
        }

        @Test
        @DisplayName("Authenticated user (any role) → should succeed")
        @WithMockUser(username = "bob", roles = "USER") // Mock authenticated user with USER role
        void privateEndpoint_authenticatedUser_shouldReturnMessage() throws Exception {
            // Act & Assert: any authenticated user should access private endpoint
            mockMvc.perform(get("/api/security/private"))
                   .andExpect(status().isOk()) // HTTP 200 OK
                   .andExpect(content().string("Dit is een besloten endpoint")); // Dutch: "This is a private endpoint"
        }
    }

    // ────────────────────────────────────────────────
    //  GET /api/security/admin - Admin endpoint tests
    // ────────────────────────────────────────────────

    @Nested // Group tests for admin endpoint
    @DisplayName("GET /api/security/admin")
    class AdminEndpoint {

        @Test
        @DisplayName("Anonymous → denied")
        @WithAnonymousUser // Mock anonymous user
        void adminEndpoint_anonymous_shouldBeDenied() throws Exception {
            // Act & Assert: anonymous users cannot access admin endpoint
            mockMvc.perform(get("/api/security/admin"))
                   .andExpect(status().is4xxClientError()); 
        }

        @Test
        @DisplayName("Regular user → forbidden (403)")
        @WithMockUser(username = "user", roles = "USER") // Mock regular user (non-admin)
        void adminEndpoint_regularUser_shouldBeForbidden() throws Exception {
            // Act & Assert: regular users should get 403 Forbidden
            mockMvc.perform(get("/api/security/admin"))
                   .andExpect(status().isForbidden()); 
        }

        @Test
        @DisplayName("Admin user → success")
        @WithMockUser(username = "admin", roles = "ADMIN") 
        void adminEndpoint_adminRole_shouldSucceed() throws Exception {
            // Act & Assert: users with ADMIN role can access admin endpoint
            mockMvc.perform(get("/api/security/admin"))
                   .andExpect(status().isOk()) // HTTP 200 OK
                   .andExpect(content().string("Dit endpoint is voor de ADMIN_ROL")); // Dutch: "This endpoint is for the ADMIN_ROLE"
        }
    }

    // ────────────────────────────────────────────────
    //  GET /api/security/hello - Personalized greeting tests
    // ────────────────────────────────────────────────

    @Nested // Group tests for hello endpoint
    @DisplayName("GET /api/security/hello")
    class HelloEndpoint {

        @Test
        @DisplayName("Authenticated → returns personalized greeting")
        @WithMockUser(username = "charlie")  // FIXED: Added this annotation - mock authenticated user
        void helloEndpoint_authenticated_shouldReturnName() throws Exception {
            // Act & Assert: authenticated users get personalized greeting
            mockMvc.perform(get("/api/security/hello"))
                   .andExpect(status().isOk()) 
                   .andExpect(content().string(containsString("Hallo charlie"))); // Dutch: "Hello charlie" (contains check)
        }

        @Test
        @DisplayName("Anonymous → should be denied")
        @WithAnonymousUser // Mock anonymous user
        void helloEndpoint_anonymous_shouldBeDenied() throws Exception {
            // Act & Assert: anonymous users cannot access hello endpoint
            mockMvc.perform(get("/api/security/hello"))
                   .andExpect(status().is4xxClientError()); // 401 or 403
        }

        @Test
        @DisplayName("WithMockUser → correct username in response")
        @WithMockUser(username = "david") // Mock authenticated user with specific username
        void helloEndpoint_withMockUser_shouldUseMockUsername() throws Exception {
            // Act & Assert: greeting should use mocked username
            mockMvc.perform(get("/api/security/hello"))
                   .andExpect(status().isOk()) 
                   .andExpect(content().string("Hallo david")); // Dutch: "Hello david" (exact match)
        }
    }
}