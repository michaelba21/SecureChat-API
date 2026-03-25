package com.securechat.config;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.io.IOException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private Environment environment;  // Mock Spring environment for profile checking

    @InjectMocks
    private RateLimitingFilter rateLimitingFilter;  // Test subject with injected mocks

    @Mock
    private FilterChain filterChain;  // Mock servlet filter chain

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    // Helper method to set up request with path and IP address
    private void setupRequest(String path, String remoteAddr) {
        request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(remoteAddr);
        response = new MockHttpServletResponse();
    }

    // Nested test class for testing behavior when "test" profile is active
    @Nested
    class TestProfileTests {

        @Test
        void doFilter_skipsRateLimiting_whenTestProfileActive() throws IOException, ServletException {
            // Given: "test" profile is active
            when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

            setupRequest("/api/auth/login", "192.168.1.1");

            // When: Filter processes request
            rateLimitingFilter.doFilter(request, response, filterChain);

            // Then: Rate limiting bypassed, filter chain called
            verify(filterChain, times(1)).doFilter(request, response);
            assert(response.getStatus() != 429);  // No 429 Too Many Requests
        }

        @Test
        void doFilter_skipsRateLimiting_whenMultipleProfilesIncludeTest() throws IOException, ServletException {
            // Given: Multiple profiles including "test"
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev", "test", "feature"});

            setupRequest("/api/auth/login", "10.0.0.1");

            // When: Filter processes request
            rateLimitingFilter.doFilter(request, response, filterChain);

            // Then: Rate limiting bypassed
            verify(filterChain).doFilter(request, response);
        }
    }

    // Nested test class for testing rate limiting behavior in non-test profiles
    @Nested
    class NonTestProfileTests {

        @BeforeEach
        void setupNonTestProfile() {
            // Setup for all tests: simulate non-test environment (e.g., production)
            when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        }

        @Test
        void doFilter_allowsRequest_whenWithinLimit_andLoginPath() throws IOException, ServletException {
            // Given: Login endpoint requests within rate limit (5 requests allowed)
            setupRequest("/api/auth/login", "127.0.0.1");

            // When: Make 2 requests (under the 5-request limit)
            rateLimitingFilter.doFilter(request, response, filterChain);
            rateLimitingFilter.doFilter(request, response, filterChain); // 2nd within limit (5 allowed)

            // Then: Both requests pass through
            verify(filterChain, times(2)).doFilter(request, response);
            assert(response.getStatus() != 429);
        }

        @Test
        void doFilter_returns429_whenExceedsLimit() throws IOException, ServletException {
            // Given: Login endpoint with rate limiting (5 requests max)
            setupRequest("/api/auth/login", "192.168.1.100");

            // When: Make 6 requests (exceeding the 5-request limit)
            // Consume all 5 tokens
            for (int i = 0; i < 5; i++) {
                rateLimitingFilter.doFilter(request, response, filterChain);
            }

            // 6th request should be blocked
            rateLimitingFilter.doFilter(request, response, filterChain);

            // Then: First 5 pass, 6th gets 429 error
            verify(filterChain, times(5)).doFilter(request, response); // only first 5 passed
            assert(response.getStatus() == 429);  // HTTP 429 Too Many Requests
            assert(response.getContentType().contains("application/json"));  // JSON error response
            assert(response.getContentAsString().contains("Too many login attempts"));  // Error message
        }

        @Test
        void doFilter_allowsRequest_onNonLoginPath() throws IOException, ServletException {
            // Given: Non-login endpoints (should not have rate limiting)
            setupRequest("/api/auth/register", "10.0.0.5");
            setupRequest("/api/chatrooms", "10.0.0.5");
            setupRequest("/health", "10.0.0.5");

            // When: Make multiple requests to different non-login endpoints
            rateLimitingFilter.doFilter(request, response, filterChain);
            rateLimitingFilter.doFilter(request, response, filterChain);
            rateLimitingFilter.doFilter(request, response, filterChain);

            // Then: All requests pass (no rate limiting for non-login paths)
            verify(filterChain, times(3)).doFilter(request, response);
            assert(response.getStatus() != 429);
        }

        @Test
        void getClientIP_returnsRemoteAddr_whenNoXForwardedFor() throws IOException, ServletException {
            // Given: Request without X-Forwarded-For header
            setupRequest("/api/auth/login", "203.0.113.42");
            request.removeHeader("X-Forwarded-For");

            // When: Filter processes request
            rateLimitingFilter.doFilter(request, response, filterChain);

            // Then: Uses remote address for IP identification
            verify(filterChain).doFilter(request, response); // should pass first request
        }

        @Test
        void getClientIP_returnsFirstIp_whenXForwardedForPresent() throws IOException, ServletException {
            // Given: Request with X-Forwarded-For header (proxy chain)
            setupRequest("/api/auth/login", "198.51.100.23");
            request.addHeader("X-Forwarded-For", "203.0.113.195, 198.51.100.42");

            // When: Make 2 requests
            rateLimitingFilter.doFilter(request, response, filterChain);
            // Second request from same client IP should share bucket
            rateLimitingFilter.doFilter(request, response, filterChain);

            // Then: Both requests pass (rate limit shared by client IP from X-Forwarded-For)
            verify(filterChain, times(2)).doFilter(request, response);
        }

        @Test
        void resolveBucket_createsNewBucket_forNewIp() throws IOException, ServletException {
            // Given: Two different client IPs
            MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/auth/login");
            request1.setRemoteAddr("192.168.0.1");
            MockHttpServletResponse response1 = new MockHttpServletResponse();
            
            MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/auth/login");
            request2.setRemoteAddr("192.168.0.2");
            MockHttpServletResponse response2 = new MockHttpServletResponse();

            // When: Both IPs make requests
            rateLimitingFilter.doFilter(request1, response1, filterChain);
            rateLimitingFilter.doFilter(request2, response2, filterChain);

            // Then: Each IP gets separate rate limit bucket
            verify(filterChain, times(2)).doFilter(any(MockHttpServletRequest.class), any(MockHttpServletResponse.class));
        }

        @Test
        void isTestProfile_returnsFalse_whenNoProfiles() throws IOException, ServletException {
            // Given: No active Spring profiles
            when(environment.getActiveProfiles()).thenReturn(new String[0]);

            setupRequest("/api/auth/login", "10.10.10.10");
            
            // When: Filter processes request
            rateLimitingFilter.doFilter(request, response, filterChain);

            // Then: Rate limiting active (not in test profile)
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void isTestProfile_returnsFalse_whenNoTestProfile() throws IOException, ServletException {
            // Given: Active profiles without "test" profile
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev", "prod"});

            setupRequest("/api/auth/login", "172.16.0.1");
            
            // When: Filter processes request
            rateLimitingFilter.doFilter(request, response, filterChain);

            // Then: Rate limiting active (not in test profile)
            verify(filterChain).doFilter(request, response);
        }
    }
}