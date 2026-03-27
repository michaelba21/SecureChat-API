package com.securechat.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // Marks this class as a REST controller (handles HTTP requests)
public class HomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE) // Handles GET requests to root path
    public String home() {
        return "SecureChat API is running"; // Simple plain text response
    }
} 