package com.securechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatRoomCreateRequest {
    @NotBlank(message = "Name cannot be blank")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;
    private String description;
    private Boolean isPrivate; // Visibility flag (true = private, false = public)
    private Integer maxParticipants;

    // Default constructor
    public ChatRoomCreateRequest() {
    }

    public ChatRoomCreateRequest(String name, String description, Boolean isPrivate, Integer maxParticipants) {
        this.name = name; // Set chat room name
        this.description = description;
        this.isPrivate = isPrivate;
        this.maxParticipants = maxParticipants; // Set participant limit
    }

    // Getters and Setters
    public String getName() {
        return name;
    } // Get chat room name

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    } // Set description

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    } // Set privacy flag

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
}