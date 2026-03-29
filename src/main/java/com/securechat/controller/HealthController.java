package com.securechat.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint for SecureChat API.
 * Provides quick verification that the service is running and authentication is
 * configured.
 * Returns immediately without requiring authentication, making it suitable for
 * load balancers and readiness probes.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Value("${spring.application.name:SecureChat API}")
    private String applicationName;

    /**
     * Simple health check endpoint.
     * 
     * @return Health status object with service information
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", applicationName);
        health.put("mode", "Keycloak-only");
        health.put("authProvider", "Keycloak (OAuth2)");
        health.put("database", "PostgreSQL");
        health.put("version", "1.0.0-keycloak-fix");

        return ResponseEntity.ok(health);
    }

    /**
     * Quick status without application.name overhead.
     * Alternative minimal endpoint for simple monitoring.
     * 
     * @return Minimal up status
     */
    @GetMapping("/basic")
    public ResponseEntity<Map<String, String>> basicHealth() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return ResponseEntity.ok(status);
    }
}
