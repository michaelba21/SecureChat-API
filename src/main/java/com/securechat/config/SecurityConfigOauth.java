package com.securechat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *  OAuth2/Keycloak security configuration for the API.
 *
 */
@Configuration
@Profile("legacy-oauth2")
public class SecurityConfigOauth {
	@Value("${keycloak.client-id:securechat-backend}")
	String clientId;

	@Value("${spring.security.oauth2.resourceserver.jwt.audiences:securechat-backend}")
	String audience;

	@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:9090/realms/securechat}")
	String issuer;
// CORS configuration to allow cross-origin requests
	@Bean
	UrlBasedCorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.setAllowedOrigins(List.of("*"));
		corsConfiguration.setAllowedMethods(List.of("*"));
		corsConfiguration.setAllowedHeaders(List.of("*"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", corsConfiguration);
		return source;
	}
// Main security configuration bean
	@Bean
	public SecurityFilterChain config(HttpSecurity http) throws Exception {
		return http
				.httpBasic(Customizer.withDefaults())
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.csrf(csrf -> csrf.disable())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt
								.jwtAuthenticationConverter(jwtAuthenticationConverter())
								.decoder(jwtDecoder())))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/security/public").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/security/admin").hasRole("ADMIN")
						.requestMatchers("/api/security/private", "/api/security/hello").authenticated()
						.anyRequest().authenticated())
				.build();
	}
// Custom JWT decoder with issuer and audience validation
	public JwtDecoder jwtDecoder() {
		NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuer);

		OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
			"aud",
			aud -> aud != null && aud.contains(audience));
			// Combine issuer validation with audience validation
		OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
		OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);
		jwtDecoder.setJwtValidator(withAudience);
		return jwtDecoder;
	}

	public JwtAuthenticationConverter jwtAuthenticationConverter() {

		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new Converter<>() {
			@Override
			public Collection<GrantedAuthority> convert(Jwt source) {
				Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
				for (String authority : getAuthorities(source)) {
					grantedAuthorities.add(new SimpleGrantedAuthority(authority));
				}
				return grantedAuthorities;
			}
	        // Extract roles from JWT token
			// Keycloak stores roles in resource_access.{clientId}.roles or realm_access.roles
			@SuppressWarnings("unchecked")
			private List<String> getAuthorities(Jwt jwt) {
				Map<String, Object> resourceAcces = jwt.getClaim("resource_access");
				if (resourceAcces != null) {
					if (resourceAcces.get(clientId) instanceof Map<?, ?>) {
						Map<String, Object> client = (Map<String, Object>) resourceAcces.get(clientId);
						if (client != null && client.containsKey("roles")) {
							return (List<String>) client.get("roles");
						}
					} else {
						// Fall back to realm roles if client roles not found
						Map<String, Object> realmAcces = jwt.getClaim("realm_access");
						if (realmAcces != null && realmAcces.containsKey("roles")) {
							return (List<String>) realmAcces.get("roles");
						}
						return new ArrayList<>();
					}
				}
				return new ArrayList<>();
			}
		});
		return jwtAuthenticationConverter;
	}
}
