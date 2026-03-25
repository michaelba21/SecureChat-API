package com.securechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

// Main application class - entry point for Spring Boot application
@SpringBootApplication  
@EnableScheduling  
public class SecureChatApplication {

  // Main method - application entry point
  public static void main(String[] args) {
    // Launches the Spring Boot application with default settings
    SpringApplication.run(SecureChatApplication.class, args);
  }
  
  /**
   * Configures CORS (Cross-Origin Resource Sharing) filter for the entire application.
   * This allows frontend applications from different domains to access the API.
   * 
   * @return 
   */
  @Bean  // Creates a Spring bean that will be automatically registered in the application context
  public CorsFilter corsFilter() {
    // Create CORS configuration with permissive settings (adjust for production!)
    CorsConfiguration config = new CorsConfiguration();
    
    // Allow cookies/authentication headers to be sent cross-origin
    config.setAllowCredentials(true);
    
    //  Allows ALL origins (use specific domains in production for security)
    config.addAllowedOriginPattern("*");
    
    // Allow ALL HTTP headers in requests (Authorization, Content-Type, etc.)
    config.addAllowedHeader("*");
    
    // Allow ALL HTTP methods (GET, POST, PUT, DELETE, OPTIONS, etc.)
    config.addAllowedMethod("*");
    
    // Create configuration source that applies these CORS rules to ALL endpoints
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);  // Apply to all paths
    
    // Return filter that will intercept and handle CORS requests
    return new CorsFilter(source);
  }
}