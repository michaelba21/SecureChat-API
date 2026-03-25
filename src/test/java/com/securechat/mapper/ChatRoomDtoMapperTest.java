package com.securechat.mapper;

import com.securechat.dto.ChatRoomCreateRequest;
import com.securechat.entity.ChatRoom;
import com.securechat.entity.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ChatRoomDtoMapperTest {
    // This test class validates the ChatRoomDtoMapper which converts ChatRoomCreateRequest DTOs
    // to ChatRoom entities, handling business logic for default values and validation
    
    // Test constants for consistent test data
    private static final String ROOM_NAME = "Developers Hangout";
    private static final String ROOM_DESCRIPTION = "A place for developers to chat";
    private static final Integer MAX_PARTICIPANTS = 100;
    private static final UUID CREATOR_ID = UUID.randomUUID();
    private static final String CREATOR_USERNAME = "alice";

    @Test
    void toEntity_mapsAllFieldsCorrectly_whenAllValuesProvided() {
        // Tests the complete mapping scenario where all optional fields are provided
        // This is the "full feature" creation path
        User creator = createCreator();

        ChatRoomCreateRequest request = new ChatRoomCreateRequest();
        request.setName(ROOM_NAME);                    // Required field
        request.setDescription(ROOM_DESCRIPTION);      
        request.setIsPrivate(true);                    // Privacy flag
        request.setMaxParticipants(MAX_PARTICIPANTS);  

        ChatRoom room = ChatRoomDtoMapper.toEntity(request, creator);

        // Verify all fields are correctly mapped from DTO to Entity
        assertThat(room)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", ROOM_NAME)
                .hasFieldOrPropertyWithValue("description", ROOM_DESCRIPTION)
                .hasFieldOrPropertyWithValue("isPrivate", true)           // Private room
                .hasFieldOrPropertyWithValue("maxParticipants", MAX_PARTICIPANTS) 
                .hasFieldOrPropertyWithValue("createdBy", creator);       // Creator relationship
    }

    @Test
    void toEntity_setsDefaultMaxParticipants_whenNull() {
        // Tests default value logic: When maxParticipants is null, should use default (50)
        // This ensures rooms have a reasonable participant limit even when not specified
        User creator = createCreator();

        ChatRoomCreateRequest request = new ChatRoomCreateRequest();
        request.setName(ROOM_NAME);
        request.setDescription(ROOM_DESCRIPTION);
        request.setIsPrivate(false);
        request.setMaxParticipants(null); // explicitly null - client didn't specify

        ChatRoom room = ChatRoomDtoMapper.toEntity(request, creator);

        assertThat(room.getMaxParticipants()).isEqualTo(50); // Default value should be applied
    }

    @Test
    void toEntity_handlesIsPrivateNullOrFalse_asFalse() {
        // Tests privacy flag logic: null or false should both result in public room (isPrivate = false)
        // This ensures backward compatibility and sensible defaults
        User creator = createCreator();

        // Case 1: null value (client didn't specify privacy setting)
        ChatRoomCreateRequest request1 = new ChatRoomCreateRequest();
        request1.setName(ROOM_NAME);
        request1.setIsPrivate(null); // Undefined privacy - should default to public

        ChatRoom room1 = ChatRoomDtoMapper.toEntity(request1, creator);
        assertThat(room1.getIsPrivate()).isFalse(); // Should default to public room

        // Case 2: explicitly false (client wants public room)
        ChatRoomCreateRequest request2 = new ChatRoomCreateRequest();
        request2.setName(ROOM_NAME);
        request2.setIsPrivate(false); // Explicitly public

        ChatRoom room2 = ChatRoomDtoMapper.toEntity(request2, creator);
        assertThat(room2.getIsPrivate()).isFalse(); // Should be public room
    }

    @Test
    void toEntity_setsIsPrivateToTrue_onlyWhenExplicitlyTrue() {
        // Tests that privacy flag is only true when explicitly set to true
        // This prevents accidental private rooms due to default values
        User creator = createCreator();

        ChatRoomCreateRequest request = new ChatRoomCreateRequest();
        request.setName(ROOM_NAME);
        request.setIsPrivate(true); // Explicitly requesting private room

        ChatRoom room = ChatRoomDtoMapper.toEntity(request, creator);

        assertThat(room.getIsPrivate()).isTrue(); // Should only be true when explicitly requested
    }

    @Test
    void toEntity_returnsNull_whenRequestIsNull() {
        // Tests null safety: Should return null when request DTO is null
        // Prevents NullPointerException in calling code
        User creator = createCreator();

        ChatRoom room = ChatRoomDtoMapper.toEntity(null, creator);

        assertThat(room).isNull(); // Should handle null request gracefully
    }

    @Test
    void toEntity_assignsCreatorCorrectly() {
        // Tests that the creator relationship is properly established
        // This is critical for authorization checks (only creator can modify room)
        User creator = createCreator();

        ChatRoomCreateRequest request = new ChatRoomCreateRequest();
        request.setName("Test Room");

        ChatRoom room = ChatRoomDtoMapper.toEntity(request, creator);

        // Verify creator is assigned and reference equality is preserved
        assertThat(room.getCreatedBy())
                .isNotNull()          
                .isSameAs(creator);   // Should be the exact same object reference
    }

    @Test
    void toEntity_worksWithMinimalRequest() {
        // Tests minimal creation scenario: only required field (name) provided
        // This is the most common use case - quick room creation
        User creator = createCreator();

        ChatRoomCreateRequest minimalRequest = new ChatRoomCreateRequest();
        minimalRequest.setName("Minimal Room Only"); // Only required field

        ChatRoom room = ChatRoomDtoMapper.toEntity(minimalRequest, creator);

        // Verify defaults are applied correctly for missing optional fields
        assertThat(room)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "Minimal Room Only")
                .hasFieldOrPropertyWithValue("description", null)     // Optional - can be null
                .hasFieldOrPropertyWithValue("isPrivate", false)    
                .hasFieldOrPropertyWithValue("maxParticipants", 50)   // Default participant limit
                .hasFieldOrPropertyWithValue("createdBy", creator);  
    }

    private User createCreator() {
        // Helper method to create a consistent test User entity
        // Used as the creator for all chat rooms in these tests
        User user = new User();
        user.setId(CREATOR_ID);                // Unique identifier
        user.setUsername(CREATOR_USERNAME);    // Display name
        return user;
    }
}