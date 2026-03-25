
package com.securechat.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class InputSanitizerTest {
    
    // The sanitizer instance to be tested - created once for all test methods
    private final InputSanitizer sanitizer = new InputSanitizer();

    @Test
    void sanitize_removesScriptTags_andMaliciousAttributes() {
        // Test input containing both script tags and malicious HTML attributes
        String malicious = "<script>alert('xss')</script><img src=x onerror=alert(1)>";
        
        // Apply sanitization to the malicious input
        String result = sanitizer.sanitize(malicious);

        // Verify that the result is empty (all malicious content removed)
        assertThat(result)
                .isEqualTo("")  // Malicious tags are completely removed
                .doesNotContain("<script>", "alert", "onerror"); // Ensure no traces remain
    }

    @Test
    void sanitize_passesThroughPlainText() {
        // Test input with plain text that includes a less-than symbol (<3)
        String safe = "Hello, this is plain text with <3 hearts!";

        // Apply sanitization to the safe input
        String result = sanitizer.sanitize(safe);

        // Verify that plain text is preserved but special characters are escaped
        assertThat(result).isEqualTo("Hello, this is plain text with &lt;3 hearts!");
    }

    @Test
    void sanitize_preservesSafeFormattingTags() {
        // Test input with allowed HTML tags (bold, italic, paragraph, anchor)
        String html = "<p><b>Bold</b> and <i>italic</i></p><a href=\"https://example.com\">link</a>";

        // Apply sanitization to the HTML input
        String result = sanitizer.sanitize(html);

        // Verify that safe tags are preserved and enhanced with security attributes
        assertThat(result)
                .contains("<b>Bold</b>")  // Bold tag preserved
                .contains("<i>italic</i>")  
                .contains("<a href=\"https://example.com\"")  // Anchor tag preserved
                .contains("href")  // href attribute preserved
                .contains("rel=\"nofollow\"")  // Security attribute added (nofollow)
                .contains("link</a>");  
    }

    @Test
    void sanitize_handlesNullInput_returnsNull() {
        // Test with null input to ensure no NullPointerException
        String result = sanitizer.sanitize(null);

        // Verify that null input returns null output
        assertThat(result).isNull();
    }

    @Test
    void sanitizeAndTrim_trimsWhitespace_afterSanitizing() {
        // Test input with surrounding whitespace and HTML content
        String input = "   <b>  Hello World  </b>   ";

        // Apply sanitizeAndTrim which should sanitize first, then trim
        String result = sanitizer.sanitizeAndTrim(input);

        // Verify that only external whitespace is trimmed, internal whitespace preserved
        assertThat(result).isEqualTo("<b>  Hello World  </b>");
    }

    @Test
    void sanitizeAndTrim_returnsNull_whenInputIsNull() {
        // Test null input with sanitizeAndTrim method
        String result = sanitizer.sanitizeAndTrim(null);

        // Verify that null input returns null output
        assertThat(result).isNull();
    }

    @Test
    void sanitizeAndTrim_handlesOnlyWhitespace() {
        // Test input containing only whitespace characters
        String input = "   \t\n   ";

        // Apply sanitizeAndTrim to whitespace-only input
        String result = sanitizer.sanitizeAndTrim(input);

        // Verify that result is empty string (trim removes all whitespace)
        assertThat(result).isEmpty();  
    }

    @Test
    void sanitize_blocksCommonXssVectors() {
        // Array of common XSS attack vectors to test
        String[] attacks = {
                "<script>evil()</script>",  
                "<img src=\"x\" onerror=\"alert(1)\">",  // Image with onerror handler
                "<iframe src=\"evil.com\"></iframe>",  // iframe injection
                "<svg onload=alert(1)>",  
                "onload=alert(1)",  // Standalone event handler
                "\"'><script>alert(1)</script>"  // HTML attribute break attempt
        };

        // Loop through each attack vector and verify sanitization
        for (String attack : attacks) {
            String sanitized = sanitizer.sanitize(attack);
            assertThat(sanitized)
                    .as("Attack vector should be neutralized: " + attack)
                    .matches(s -> {
                        // If string is empty or blank, it's safe (malicious tags removed)
                        if (s.isEmpty() || s.isBlank()) {
                            return true;
                        }
                        // For non-empty strings, check that dangerous tags are not present
                        boolean noDangerousTags = !s.contains("<script>") && 
                                                !s.contains("<img") && 
                                                !s.contains("<iframe") && 
                                                !s.contains("<svg");
                        
                        // If dangerous attributes are present, they should be escaped
                        // The equals sign (=) would be encoded as "&#61;" if escaped
                        boolean attributesEscaped = (!s.contains("onload=") || s.contains("&#61;")) &&
                                                  (!s.contains("onerror=") || s.contains("&#61;"));
                        
                        return noDangerousTags && attributesEscaped;
                    });
        }
        
        // Test that plain text protocols are preserved (not XSS)
        // Note: This tests that "javascript:" in text is not treated as XSS
        String plainTextAttack = "javascript:alert(1)";
        String sanitized = sanitizer.sanitize(plainTextAttack);
        assertThat(sanitized).isEqualTo("javascript:alert(1)");
    }
}