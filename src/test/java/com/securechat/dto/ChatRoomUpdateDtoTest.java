
package com.securechat.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ChatRoomUpdateDto → Full validation & record coverage") // Test class description
class ChatRoomUpdateDtoTest {

    private static Validator validator; // JSR-380 Validator instance

    @BeforeAll // Runs once before all tests
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory(); // Create validator factory
        validator = factory.getValidator(); // Get validator instance
    }

    // Helper method to validate DTO and return constraint violations
    private Set<ConstraintViolation<ChatRoomUpdateDto>> validate(ChatRoomUpdateDto dto) {
        return validator.validate(dto); // Validate DTO against annotations
    }

    // FIXED: Updated to handle multiple violations for empty/blank names
    private void assertOneViolation(ChatRoomUpdateDto dto, String expectedMessage) {
        var violations = validate(dto); // Get violations
        
        // For empty/blank strings, we might get 2 violations (@NotBlank + @Size(min=3))
        // So we check that we have at least one violation with the expected message
        assertFalse(violations.isEmpty(), "Should have at least one violation"); 
        
        List<String> messages = violations.stream()
            .map(ConstraintViolation::getMessage) // Extract messages
            .toList(); // Convert to list
        
        assertTrue(messages.contains(expectedMessage), // Check if expected message exists
            "Should have violation with message: " + expectedMessage + 
            ". Actual messages: " + messages); // Error message with actual messages
    }

    // ────────────────────────────────────────────────
    //  name field – @NotBlank + @Size(min=3, max=100)
    //  Tests for name field validation
    // ────────────────────────────────────────────────

    @Test
    @DisplayName("name = null → fails @NotBlank")
    void name_null_shouldFailNotBlank() {
        // Arrange: create DTO with null name
        var dto = new ChatRoomUpdateDto(null, "desc", true, 50);
        
        // Act & Assert: should have @NotBlank violation
        assertOneViolation(dto, "Name is required");
    }

    @Test
    @DisplayName("name = empty string → fails @NotBlank")
    void name_empty_shouldFailNotBlank() {
        // Arrange: create DTO with empty name
        var dto = new ChatRoomUpdateDto("", "desc", true, 50);
        
        // Act & Assert: empty string should trigger @NotBlank violation
        var violations = validate(dto);
        assertTrue(violations.size() >= 1, "Should have at least one violation for empty name");
        
        // Check specifically for @NotBlank message
        boolean hasNotBlankMessage = violations.stream()
            .anyMatch(v -> v.getMessage().equals("Name is required"));
        assertTrue(hasNotBlankMessage, "Should have @NotBlank violation");
    }

    @Test
    @DisplayName("name = whitespace only → fails @NotBlank")
    void name_blank_shouldFailNotBlank() {
        // Arrange: create DTO with whitespace-only name
        var dto = new ChatRoomUpdateDto("   ", "desc", true, 50);
        
        // Act & Assert: whitespace should trigger @NotBlank violation
        assertOneViolation(dto, "Name is required");
    }

    @ParameterizedTest(name = "name length {0} → {1}") // Parameterized test with display name
    @MethodSource("nameLengthProvider") 
    void name_length_validation(String name, boolean shouldBeValid) {
        // Arrange: create DTO with test name
        var dto = new ChatRoomUpdateDto(name, "desc", true, 50);

        // Act: validate DTO
        var violations = validate(dto);
        
        // Assert: check validation result
        if (shouldBeValid) {
            assertThat(violations).isEmpty(); // Should have no violations
        } else {
            assertThat(violations).isNotEmpty(); // Should have violations
            
            // Check if it's a @Size violation (not @NotBlank)
            boolean isSizeViolation = violations.stream()
                .anyMatch(v -> v.getMessage().contains("between 3 and 100") || 
                              v.getMessage().contains("must be between")); // @Size message
            assertTrue(isSizeViolation, "Should be a size violation for length: " + name.length());
        }
    }

    // Provides test data for name length validation
    static Stream<Arguments> nameLengthProvider() {
        return Stream.of(
                Arguments.of("ab",       false),   
                Arguments.of("abc",      true),    // minimum length (3 chars) - valid
                Arguments.of("a".repeat(100), true), 
                Arguments.of("a".repeat(101), false)  // too long (101 chars) - invalid
        );
    }

    // ────────────────────────────────────────────────
    //  description – @Size(max=500)   (optional)
    //  Tests for description field validation
    // ────────────────────────────────────────────────

    @Test
    @DisplayName("description = null → valid")
    void description_null_isValid() {
        // Arrange: DTO with null description
        var dto = new ChatRoomUpdateDto("Valid name", null, true, 50);
        
        // Act & Assert: null description should be valid (optional field)
        assertThat(validate(dto)).isEmpty();
    }

    @Test
    @DisplayName("description = empty → valid")
    void description_empty_isValid() {
        // Arrange: DTO with empty description
        var dto = new ChatRoomUpdateDto("Valid name", "", true, 50);
        
        // Act & Assert: empty description should be valid
        assertThat(validate(dto)).isEmpty();
    }

    @Test
    @DisplayName("description = 500 chars → valid")
    void description_maxLength_valid() {
        // Arrange: DTO with description at maximum length
        var dto = new ChatRoomUpdateDto("Valid name", "a".repeat(500), true, 50);
        
        // Act & Assert: 500 characters should be valid (max limit)
        assertThat(validate(dto)).isEmpty();
    }

    @Test
    @DisplayName("description = 501 chars → invalid")
    void description_tooLong_fails() {
        // Arrange: DTO with description exceeding max length
        var dto = new ChatRoomUpdateDto("Valid name", "a".repeat(501), true, 50);
        
        // Act: validate DTO
        var violations = validate(dto);
        
        // Assert: should have one violation
        assertEquals(1, violations.size()); // Exactly one violation
        assertThat(violations.iterator().next().getMessage())
                .contains("cannot exceed 500"); // Check error message
    }

    // ────────────────────────────────────────────────
    //  isPrivate – Boolean (wrapper) → can be null
    //  Tests for isPrivate field validation
    // ────────────────────────────────────────────────

    @Test
    @DisplayName("isPrivate = null → valid")
    void isPrivate_null_isValid() {
        // Arrange: DTO with null isPrivate
        var dto = new ChatRoomUpdateDto("Valid name", "desc", null, 50);
        
        // Act & Assert: null isPrivate should be valid (optional)
        assertThat(validate(dto)).isEmpty();
    }

    @Test
    @DisplayName("isPrivate = true/false → valid")
    void isPrivate_booleanValues_valid() {
        // Arrange: DTOs with boolean values
        var dto1 = new ChatRoomUpdateDto("Valid name", "desc", true, 50); // True
        var dto2 = new ChatRoomUpdateDto("Valid name", "desc", false, 50); // False
        
        // Act & Assert: both should be valid
        assertThat(validate(dto1)).isEmpty();
        assertThat(validate(dto2)).isEmpty();
    }

    // ────────────────────────────────────────────────
    //  maxParticipants – @PositiveOrZero + @Max(1000)
    //  Tests for maxParticipants field validation
    // ────────────────────────────────────────────────

    @ParameterizedTest(name = "maxParticipants = {0} → {1}")
    @MethodSource("maxParticipantsProvider") // Uses method source
    void maxParticipants_validation(Integer value, boolean shouldBeValid) {
        // Arrange: create DTO with test value
        var dto = new ChatRoomUpdateDto("Valid name", "desc", true, value);

        // Act: validate DTO
        var violations = validate(dto);
        
        // Assert: check validation result
        if (shouldBeValid) {
            assertThat(violations).isEmpty(); // Should be valid
        } else {
            assertThat(violations).isNotEmpty(); // Should be invalid
        }
    }

    // Provides test data for maxParticipants validation
    static Stream<Arguments> maxParticipantsProvider() {
        return Stream.of(
                Arguments.of(null,                 true),   
                Arguments.of(0,                    true),   // zero - valid (PositiveOrZero)
                Arguments.of(1,                    true),   // positive - valid
                Arguments.of(999,                  true),   
                Arguments.of(1000,                 true),   // at max - valid
                Arguments.of(1001,                 false), 
                Arguments.of(-1,                   false)   // negative - invalid
        );
    }

    // ────────────────────────────────────────────────
    //  Record features: equals, hashCode, toString, components
    //  Tests for Java record features (auto-generated methods)
    // ────────────────────────────────────────────────

    @Test
    @DisplayName("equals & hashCode – same values → equal")
    void equals_and_hashCode_sameValues_equal() {
        // Arrange: two DTOs with same values
        var a = new ChatRoomUpdateDto("Room A", "Desc", true, 200);
        var b = new ChatRoomUpdateDto("Room A", "Desc", true, 200);

        // Act & Assert: should be equal and have same hash code
        assertEquals(a, b); // equals() should return true
        assertEquals(a.hashCode(), b.hashCode()); // hashCode() should be equal
    }

    @Test
    @DisplayName("equals – different name → not equal")
    void equals_differentName_notEqual() {
        // Arrange: two DTOs with different names
        var a = new ChatRoomUpdateDto("Room A", "Desc", true, 200);
        var b = new ChatRoomUpdateDto("Room B", "Desc", true, 200);

        // Act & Assert: should not be equal
        assertNotEquals(a, b); // equals() should return false
    }

    @Test
    @DisplayName("toString contains all fields")
    void toString_containsAllFields() {
        // Arrange: create DTO
        var dto = new ChatRoomUpdateDto("Test Room", "My room", false, 150);

        // Act: call toString()
        String str = dto.toString();
        
        // Assert: should contain all field values
        assertThat(str).contains("Test Room", "My room", "false", "150");
    }

    @Test
    @DisplayName("component accessors return correct values")
    void componentAccessors_work() {
        // Arrange: create DTO
        var dto = new ChatRoomUpdateDto("Club", "Secret club", true, 30);

        // Act & Assert: record accessors should return correct values
        assertEquals("Club", dto.name()); // name accessor
        assertEquals("Secret club", dto.description()); // description accessor
        assertEquals(true, dto.isPrivate()); 
        assertEquals(30, dto.maxParticipants()); // maxParticipants accessor
    }
    
    // NEW: Add a test to verify the actual behavior for empty string
    @Test
    @DisplayName("Debug: Check what violations empty name produces")
    void debug_emptyNameViolations() {
        // Arrange: DTO with empty name
        var dto = new ChatRoomUpdateDto("", "desc", true, 50);
        
        // Act: validate and print violations
        var violations = validate(dto);
        
        // Debug output (commented out in production)
        System.out.println("Empty name violations (" + violations.size() + "):");
        violations.forEach(v -> {
            System.out.println("  - Property: " + v.getPropertyPath());
            System.out.println("    Message: " + v.getMessage());
            System.out.println("    Validator: " + v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName());
        });
        
        // Assert: should have at least one violation
        assertTrue(violations.size() > 0);
    }
}