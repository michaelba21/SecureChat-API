package com.securechat.service;

import com.securechat.entity.RefreshToken;
import com.securechat.entity.User;
import com.securechat.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    // This test class validates the RefreshTokenService which manages
    // refresh tokens for JWT authentication (token rotation, validation, cleanup)

    @Mock
    private RefreshTokenRepository tokenRepository;  // Repository for refresh token persistence

    @InjectMocks
    private RefreshTokenService refreshTokenService;  // Service under test

    private User user;
    private static final String IP_ADDRESS = "192.168.1.1";  // Client IP for security logging
    private static final String USER_AGENT = "Mozilla/5.0 Test";  

    @BeforeEach
    void setUp() {
        user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());  // Set private ID field
    }

    @Test
    void createToken_whenLessThan5Tokens_createsNewToken() {
        // Tests creating a new token when user has fewer than maximum allowed tokens
        when(tokenRepository.findByUser(user)).thenReturn(List.of());  // User has 0 existing tokens
        when(tokenRepository.saveAndFlush(any())).thenAnswer(i -> i.getArguments()[0]);  

        RefreshToken token = refreshTokenService.createToken(user, IP_ADDRESS, USER_AGENT);

        // Verify token properties
        assertNotNull(token.getToken());  // Token should be generated
        assertEquals(user, token.getUser());  
        assertEquals(IP_ADDRESS, token.getIpAddress());  
        assertEquals(USER_AGENT, token.getUserAgent());  // User agent logged for security
        // Verify expiry is approximately 7 days from now (with 2 second tolerance)
        assertWithinRange(token.getExpiryDate(), Instant.now().plus(7, ChronoUnit.DAYS), 2);
        verify(tokenRepository, never()).delete(any());  // No deletion needed (under limit)
    }

    @Test
    void createToken_whenExactly5Tokens_deletesOldestAndCreatesNew() {
        // Tests token rotation: when user has maximum tokens (5), oldest is deleted
        // Create 5 tokens with different creation dates (oldest is 10 days ago)
        List<RefreshToken> existing = createTokensWithCreationTimes(
                daysAgo(10), daysAgo(8), daysAgo(6), daysAgo(4), daysAgo(2)
        );
        when(tokenRepository.findByUser(user)).thenReturn(existing);
        when(tokenRepository.saveAndFlush(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        refreshTokenService.createToken(user, IP_ADDRESS, USER_AGENT);

        // Verify oldest token (10 days ago) was deleted
        ArgumentCaptor<RefreshToken> deletedCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokenRepository).delete(deletedCaptor.capture());

        // Compare creation dates (truncated to days for reliable comparison)
        Instant expected = daysAgo(10).truncatedTo(ChronoUnit.DAYS);
        Instant actual = deletedCaptor.getValue().getCreatedAt().truncatedTo(ChronoUnit.DAYS);
        assertEquals(expected, actual);  // Oldest token should be deleted
    }

    @Test
    void createToken_whenMoreThan5Tokens_deletesOnlyOldest() {
        // Tests scenario where somehow user has more than 5 tokens (edge case)
        List<RefreshToken> existing = createTokensWithCreationTimes(
                daysAgo(15), daysAgo(12), daysAgo(9), daysAgo(6), daysAgo(3), daysAgo(1)
        );
        when(tokenRepository.findByUser(user)).thenReturn(existing);
        when(tokenRepository.saveAndFlush(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        refreshTokenService.createToken(user, IP_ADDRESS, USER_AGENT);

        // Should delete exactly 1 token (the oldest)
        verify(tokenRepository, times(1)).delete(any());
    }

    @Test
    void createToken_userAgentNullOrBlank_setsToUnknown() {
        // Tests handling of missing user agent (security logging)
        when(tokenRepository.findByUser(user)).thenReturn(List.of());
        when(tokenRepository.saveAndFlush(any())).thenAnswer(i -> i.getArguments()[0]);

        RefreshToken token1 = refreshTokenService.createToken(user, IP_ADDRESS, null);
        RefreshToken token2 = refreshTokenService.createToken(user, IP_ADDRESS, "   ");

        assertEquals("unknown", token1.getUserAgent());  // Null should become "unknown"
        assertEquals("unknown", token2.getUserAgent());  // Blank/whitespace should become "unknown"
    }

    @Test
    void validateAndRefresh_validToken_updatesLastUsedAndReturnsIt() {
        // Tests successful token validation and refresh (token rotation)
        RefreshToken token = createValidToken();
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        Optional<RefreshToken> result = refreshTokenService.validateAndRefresh("valid-token");

        assertTrue(result.isPresent());  // Should return token
        // LastUsedAt should be updated to approximately now
        assertWithinRange(result.get().getLastUsedAt(), Instant.now(), 2);
        verify(tokenRepository).save(token);  // Should save with updated lastUsedAt
    }

    @Test
    void validateAndRefresh_tokenNotFound_returnsEmpty() {
        // Tests validation of non-existent token
        when(tokenRepository.findByToken("unknown")).thenReturn(Optional.empty());
        assertTrue(refreshTokenService.validateAndRefresh("unknown").isEmpty());  // Should return empty
    }

    @Test
    void validateAndRefresh_expiredToken_returnsEmpty() {
        // Tests validation of expired token
        RefreshToken expired = createValidToken();
        ReflectionTestUtils.setField(expired, "expiryDate", Instant.now().minus(1, ChronoUnit.DAYS));  // Expired 1 day ago
        when(tokenRepository.findByToken("expired")).thenReturn(Optional.of(expired));

        assertTrue(refreshTokenService.validateAndRefresh("expired").isEmpty());  // Should return empty
    }

    @Test
    void validateAndRefresh_tokenWithWhitespace_trimsAndValidates() {
        // Tests token trimming (handles client-side whitespace)
        RefreshToken token = createValidToken();
        token.setToken("valid-with-space"); // clean token in DB

        when(tokenRepository.findByToken("valid-with-space")).thenReturn(Optional.of(token));
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        // Pass token with surrounding whitespace
        Optional<RefreshToken> result = refreshTokenService.validateAndRefresh("  valid-with-space  ");

        assertTrue(result.isPresent());  // Should find and validate
        verify(tokenRepository).findByToken("valid-with-space"); // Proves trimming occurred
    }

    @Test
    void validateAndRefresh_blankOrNullToken_returnsEmpty() {
        // Tests edge cases: null, empty, or whitespace-only tokens
        assertTrue(refreshTokenService.validateAndRefresh(null).isEmpty());
        assertTrue(refreshTokenService.validateAndRefresh("").isEmpty());
        assertTrue(refreshTokenService.validateAndRefresh("   ").isEmpty());
        // Should handle gracefully without hitting database
    }

    @Test
    void revokeToken_existingToken_deletesIt() {
        // Tests revoking (deleting) a specific token (e.g., on logout)
        RefreshToken token = createValidToken();
        when(tokenRepository.findByToken("token-to-revoke")).thenReturn(Optional.of(token));
        
        refreshTokenService.revokeToken("token-to-revoke");
        
        verify(tokenRepository).delete(token);  // Should delete the token
    }

    @Test
    void revokeAllUserTokens_deletesAllForUser() {
        // Tests revoking all tokens for a user (e.g., password change, security breach)
        UUID userId = UUID.randomUUID();
        refreshTokenService.revokeAllUserTokens(userId);
        
        verify(tokenRepository).deleteAllByUserId(userId);  // Should delete all user's tokens
    }

    @Test
    void deleteExpiredTokens_callsRepository() {
        // Tests scheduled cleanup of expired tokens
        refreshTokenService.deleteExpiredTokens();
        
        verify(tokenRepository).deleteByExpiryDateBefore(any(Instant.class));  // Should delete expired tokens
        // Note: Uses any(Instant.class) - would be "now" in actual implementation
    }

    // ====================== HELPER METHODS ======================

    // Creates a valid refresh token with current expiry
    private RefreshToken createValidToken() {
        RefreshToken token = new RefreshToken();
        token.setToken("valid-token");
        token.setUser(user);
        token.setExpiryDate(Instant.now().plus(7, ChronoUnit.DAYS));  // 7 days from now
        token.setCreatedAt(Instant.now());
        token.setIpAddress(IP_ADDRESS);
        token.setUserAgent(USER_AGENT);
        return token;
    }

    // Creates a list of tokens with specific creation times (for testing rotation)
    private List<RefreshToken> createTokensWithCreationTimes(Instant... times) {
        List<RefreshToken> tokens = new ArrayList<>();
        for (Instant time : times) {
            RefreshToken t = createValidToken();
            ReflectionTestUtils.setField(t, "createdAt", time);  // Set private createdAt field
            tokens.add(t);
        }
        return tokens;
    }

    // Helper to calculate instant N days ago
    private Instant daysAgo(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }

    // Assertion helper for time comparisons with tolerance
    private void assertWithinRange(Instant actual, Instant expected, long secondsTolerance) {
        long diff = Math.abs(java.time.Duration.between(actual, expected).getSeconds());
        assertTrue(diff <= secondsTolerance,
                () -> "Diff: " + diff + "s, expected within " + secondsTolerance + "s");
    }
}