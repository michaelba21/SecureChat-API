package com.securechat.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger Configuration for SecureChat API
 * Makes the API visible and testable at: http://localhost:8080/swagger-ui.html
 * 
 * Features:
 * - Shows all endpoints with documentation
 * - Allows quick testing from browser
 * - Professional appearance for examiners
 * - Keycloak authentication integration
 */
@Configuration
public class OpenAPIConfig {

    /**
     * Configure OpenAPI documentation
     * Accessible at: http://localhost:8080/swagger-ui.html
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // API Info
                .info(new Info()
                        .title("SecureChat API")
                        .version("1.0.0")
                        .description("Secure chat application with Keycloak OAuth2 authentication\n\n" +
                                "## Authentication\nAll endpoints (except /api/public/**) require a valid Keycloak JWT token.\n\n" +
                                "## Key Features\n" +
                                "- Keycloak OAuth2 authentication\n" +
                                "- UUID-based user identification\n" +
                                "- Chat rooms and messaging\n" +
                                "- File uploads and sharing\n\n" +
                                "## Getting Started\n" +
                                "1. Get token from Keycloak: POST /realms/SecureChat/protocol/openid-connect/token\n" +
                                "2. Verify authentication: GET /api/auth/whoami\n" +
                                "3. Get user profile: GET /api/users/me\n" +
                                "4. Create chatroom: POST /api/chatrooms")
                        .termsOfService("http://swagger.io/terms/")
                        .contact(new Contact()
                                .name("SecureChat API Support")
                                .email("support@securechat.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))

                // Security Scheme - Bearer Token (Keycloak JWT)
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your Keycloak JWT access token. " +
                                        "Get one from: POST /realms/SecureChat/protocol/openid-connect/token")));
    }
}
