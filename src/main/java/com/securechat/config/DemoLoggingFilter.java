package com.securechat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.core.annotation.Order;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Demo Logging Filter - Shows examiner exactly what's happening during demo
 * Logs all API calls with response status and timing
 * 
 * Console output example:
 * GET /api/auth/whoami
 * GET /api/auth/whoami (45ms) 200
 */
@Component
@Order(1) // Run first before other filters
public class DemoLoggingFilter extends OncePerRequestFilter {

    private static final Logger DEMO_LOG = LoggerFactory.getLogger("DEMO");
    private static final Logger LOG = LoggerFactory.getLogger(DemoLoggingFilter.class);

    private final AtomicLong requestCount = new AtomicLong(0);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Only log API endpoints
        if (!request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        long requestId = requestCount.incrementAndGet();
        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUri = queryString != null ? uri + "?" + queryString : uri;

        // Log incoming request
        DEMO_LOG.info(" [{}] {} {}", requestId, method, fullUri);

        // Wrap response to capture status code
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            chain.doFilter(request, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = wrappedResponse.getStatus();

            // CRITICAL: Copy cached body back to actual response before returning to client
            wrappedResponse.copyBodyToResponse();

            // Determine emoji based on status code
            String statusEmoji;
            if (status < 300) {
                statusEmoji = "✅"; // Success (2xx)
            } else if (status < 400) {
                statusEmoji = "⚠️ "; // Redirect (3xx)
            } else if (status == 401 || status == 403) {
                statusEmoji = "🔒"; // Auth/Authz (401/403)
            } else if (status == 404) {
                statusEmoji = "❓"; // Not found
            } else {
                statusEmoji = "❌"; // Error (5xx)
            }

            // Log response with timing
            DEMO_LOG.info("{} [{}] {} {} ({}ms) HTTP {}", statusEmoji, requestId, method, fullUri, duration, status);

            // Log warnings for error statuses
            if (status >= 400) {
                LOG.warn("Request {} resulted in HTTP {}", requestId, status);
            }
        }
    }

}
