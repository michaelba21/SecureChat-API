package com.securechat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * I have made Input DTO here for updating a chatroom (PUT /api/chatrooms/{id}).
 * This only includes fields that the user is allowed to modify.  
 * All fields are optional except 'name' (you must provide a new name).
 */
public record ChatRoomUpdateDto(

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    String name,

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    String description,

    // Use @JsonProperty to control exact JSON field name
    // Choose one style and stick to it across create/update
    @JsonProperty("isPrivate")
    Boolean isPrivate,

    @PositiveOrZero(message = "Max participants must be >= 0")
    @Max(value = 1000, message = "Max participants cannot exceed 1000")
    Integer maxParticipants

) {}