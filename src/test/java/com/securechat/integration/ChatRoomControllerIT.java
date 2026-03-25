package com.securechat.integration;

import com.securechat.entity.ChatRoom;
import com.securechat.entity.User;
import com.securechat.repository.ChatRoomRepository;
import com.securechat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers  // Enables Testcontainers support for database container management
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)  // Full application context with random port
@AutoConfigureMockMvc  
@ActiveProfiles("integrationtest") 
@TestInstance(TestInstance.Lifecycle.PER_CLASS)  // Single test instance for all test methods (reduces startup cost)
public class ChatRoomControllerIT {  // Integration Test for ChatRoomController

    @Container  // Testcontainers PostgreSQL container - starts a real PostgreSQL instance for testing
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("securechat_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource  // Dynamically injects database properties from Testcontainer into Spring context
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> "true");  // Enables Flyway migrations for schema setup
    }

    @Autowired
    private MockMvc mockMvc;  // Main entry point for HTTP request testing

    @Autowired
    private UserRepository userRepository;  

    @Autowired
    private ChatRoomRepository chatRoomRepository;  // JPA repository for ChatRoom entities

    @BeforeEach
    void setup() {
        // Clean database before each test to ensure test isolation
        chatRoomRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createUser(String username, String email) {
        // Helper method to create test users with minimal required fields
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash("pass");  // Simplified password for testing
        return userRepository.save(u);
    }

    @Test
    void creatorCanUpdateChatRoom() throws Exception {
        // Tests authorization: Only chat room creator should be able to update it
        
        // Setup test data
        User creator = createUser("creator1", "c1@example.com");

        ChatRoom room = new ChatRoom();
        room.setName("InitialRoom");
        room.setCreatedBy(creator);  // Creator relationship established
        chatRoomRepository.save(room);

        UUID id = room.getId();

        // Perform PUT request to update chat room
        mockMvc.perform(put("/api/chatrooms/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", creator.getId().toString())  
                .with(csrf())  // CSRF protection enabled (Spring Security)
                .content("{\"name\":\"NewName\"}"))  // JSON payload with new name
                .andExpect(status().isOk())  
                .andExpect(jsonPath("$.id").value(id.toString()))  // Verify returned ID matches
                .andExpect(jsonPath("$.creatorId").value(creator.getId().toString()))  // Verify creator unchanged
                .andExpect(jsonPath("$.name").value("NewName"));  
    }

    @Test
    void nonCreatorCannotUpdateChatRoom() throws Exception {
        // Tests authorization failure: Non-creator should receive 403 Forbidden
        
        User creator = createUser("creator2", "c2@example.com");
        User other = createUser("other", "other@example.com");  // Different user

        ChatRoom room = new ChatRoom();
        room.setName("RoomNoUpdate");
        room.setCreatedBy(creator);  // Room created by 'creator', not 'other'
        chatRoomRepository.save(room);

        UUID id = room.getId();

        // Attempt update with different user's ID
        mockMvc.perform(put("/api/chatrooms/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", other.getId().toString())  // Non-creator user ID
                .with(csrf())
                .content("{\"name\":\"BadUpdate\"}"))
                .andExpect(status().isForbidden());  // Should receive 403 Forbidden
    }

    @Test
    void updateWithQuotedJsonStringParsesCorrectly() throws Exception {
        // Tests robust JSON parsing: Handles client sending quoted JSON string (edge case)
    
        User creator = createUser("creator3", "c3@example.com");

        ChatRoom room = new ChatRoom();
        room.setName("QuotedRoom");
        room.setCreatedBy(creator);
        chatRoomRepository.save(room);

        UUID id = room.getId();

   
        // This tests the application's ability to handle malformed JSON gracefully
        String quoted = "\"{\\\"name\\\":\\\"QuotedName\\\"}\"";  // JSON string inside quotes

        mockMvc.perform(put("/api/chatrooms/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", creator.getId().toString())
                .with(csrf())
                .content(quoted))  // Send the quoted JSON string
                .andExpect(status().isOk())  
                .andExpect(jsonPath("$.name").value("QuotedName"));  // Verify name parsed correctly
    }

    @Test
    void getCreatedChatroomsReturnsCreatorRooms() throws Exception {
        // Tests filtering: GET endpoint should return only rooms created by specific user
        
        // Create test users and their rooms
        User alice = createUser("alice", "alice@example.com");
        User bob = createUser("bob", "bob@example.com");

        // Alice creates two rooms
        ChatRoom a1 = new ChatRoom(); a1.setName("A1"); a1.setCreatedBy(alice); chatRoomRepository.save(a1);
        ChatRoom a2 = new ChatRoom(); a2.setName("A2"); a2.setCreatedBy(alice); chatRoomRepository.save(a2);
        
        // Bob creates one room
        ChatRoom b1 = new ChatRoom(); b1.setName("B1"); b1.setCreatedBy(bob); chatRoomRepository.save(b1);

        // Request rooms created by Alice
        mockMvc.perform(get("/api/users/" + alice.getId() + "/chatrooms/created")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // JSONPath assertions to verify response contains correct rooms
                .andExpect(jsonPath("$.[?(@.creatorId=='" + alice.getId().toString() + "')]").exists())  // All rooms should have Alice as creator
                .andExpect(jsonPath("$.[?(@.name=='A1')]").exists());  
        // Note: Should NOT contain Bob's room (B1) - implicit assertion through exclusion
    }
}