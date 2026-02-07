package com.securechat.util;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

/**
 * Utility class for sanitizing user input to prevent XSS attacks
 * Uses OWASP Java HTML Sanitizer with a secure whitelist policy suitable for chat messages
 */
@Component
public class InputSanitizer {

    private static final PolicyFactory POLICY = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    /**
     * Sanitizes user input, allowing safe formatting while blocking XSS
     *
     * @param input The raw user input
     * @return Sanitized string safe for display in HTML context
     */
    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return POLICY.sanitize(input);
    }

    /**
     * Sanitizes and trims whitespace
     *
     * @param input The raw user input
     * @return Sanitized and trimmed string
     */
    public String sanitizeAndTrim(String input) {
        String sanitized = sanitize(input);
        return sanitized != null ? sanitized.trim() : null;
    }
}