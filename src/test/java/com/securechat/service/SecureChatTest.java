
package com.securechat.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest  // Loads complete Spring Boot application context with all beans configured
@ActiveProfiles("test")  // Activates 'test' profile for test-specific configuration (e.g., H2 database)
class SecureChatTest {

	@Test
	void contextLoads() {
		// This test verifies that the Spring application context loads successfully
	}
}
