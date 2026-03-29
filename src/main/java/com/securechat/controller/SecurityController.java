package com.securechat.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security") // Base path for security-related endpoints
public class SecurityController {
	// Public endpoint - accessible without authenticat
	@GetMapping("/public")
	public String publiek() {
		return "Dit is een publiek endpoint";
	}
	// Private endpoint - requires authentication (any authenticated user)
	@GetMapping("/private")
	public String geheim() {
		return "Dit is een besloten endpoint";
	}
// Admin endpoint - requires ADMIN role (configured in SecurityConfig)
	@GetMapping("/admin")
	public String admin() {
		return "Dit endpoint is voor de ADMIN_ROL";
	}
// Personalized greeting endpoint - shows authenticated user's name
	@GetMapping("/hello")
	public String hallo(Authentication authentication) {
		String naam = authentication.getName();
		return "Hallo " + naam;
	}
}
