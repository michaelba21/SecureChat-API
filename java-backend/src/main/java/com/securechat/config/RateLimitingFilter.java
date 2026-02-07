package com.securechat.config;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap; 
/**
 * I used here the rate limiting filter for authentication endpoints
 * Implements FE-AUTH-007: Max 5 login attempts per minute per IP
 * Rate limiting is disabled in test profile to prevent interference with automated tests
 */
@Component
public class RateLimitingFilter implements Filter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>(); // IP into Bucket cache
    private final Environment environment; // Spring environment for profile detection

    public RateLimitingFilter(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        // Skip rate limiting in test profile to prevent interference with automated tests
        if (isTestProfile()) {
            chain.doFilter(request, response); // Bypass rate limiting during tests
            return;
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request; // Cast to HTTP request
        HttpServletResponse httpResponse = (HttpServletResponse) response; 
        
        String path = httpRequest.getRequestURI(); // Get request path
        
        // Only apply rate limiting to login endpoints
        if (path.contains("/api/auth/login")) { // Check if request is for login endpoint
            String clientIp = getClientIP(httpRequest); // Extract client IP address
            Bucket bucket = resolveBucket(clientIp); 
            
            if (bucket.tryConsume(1)) { 
                chain.doFilter(request, response); 
            } else {
                httpResponse.setStatus(429); // Too Many Requests - HTTP 429
                httpResponse.setContentType("application/json"); // Set JSON response type
                httpResponse.getWriter().write(
                    "{\"error\": \"Too many login attempts. Please try again later.\"}" // Error message
                );
            }
        } else {
            chain.doFilter(request, response); // Non-login endpoints bypass rate limiting
        }
    }
    
    /**
     * Check if running in test profile
     */
    private boolean isTestProfile() {
        String[] activeProfiles = environment.getActiveProfiles(); // Get active Spring profiles
        for (String profile : activeProfiles) {
            if ("test".equals(profile)) { 
                return true; // Rate limiting disabled in test profile
            }
        }
        return false; // Rate limiting enabled for non-test profiles
    }

    private Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, k -> createNewBucket()); // Create bucket if missing
    }

    private Bucket createNewBucket() {
        // 5 requests per minute - Bucket4j configuration
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))); // 5 tokens refilled every minute
        return Bucket.builder()
                .addLimit(limit) 
                .build();
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For"); // Check for proxy header
        if (xfHeader == null) {
            return request.getRemoteAddr(); // Use direct client IP if no proxy
        }
        return xfHeader.split(",")[0]; 
    }
} 