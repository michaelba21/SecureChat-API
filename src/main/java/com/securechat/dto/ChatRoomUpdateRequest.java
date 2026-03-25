package com.securechat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatRoomUpdateRequest {

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name; // Updated chatroom name - required with length validation

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Boolean isPrivate; // Privacy toggle - true for private, false for public

    @Min(value = 1, message = "Max participants must be at least 1")
    @Max(value = 1000, message = "Max participants cannot exceed 1000")
    private Integer maxParticipants; // Capacity limit with range validation (1-1000)

    // Default constructor
    public ChatRoomUpdateRequest() {
    }

    // Parameterized constructor for testing or direct instantiation
    public ChatRoomUpdateRequest(String name, String description, Boolean isPrivate, Integer maxParticipants) {
        this.name = name;
        this.description = description;
        this.isPrivate = isPrivate;
        this.maxParticipants = maxParticipants;
    }

    // Standard getters and setters for all fields
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
}
