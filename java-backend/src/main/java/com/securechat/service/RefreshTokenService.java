package com.securechat.service;
import com.securechat.entity.RefreshToken;
import com.securechat.entity.User;
import com.securechat.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional  // All methods run within database transactions
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    @Autowired
    private RefreshTokenRepository tokenRepository;  // Data access layer for refresh tokens

    /**
     * Creates a new refresh token for a user with device tracking information.
     * Implements security policy: maximum 5 active tokens per user (removes oldest).
     */
    public RefreshToken createToken(User user, String ipAddress, String userAgent) {
        // Check current token count for this user (prevent token flooding)
        List<RefreshToken> existing = tokenRepository.findByUser(user);
        if (existing.size() >= 5) {
            // Security policy: remove oldest token when user has 5 active tokens
            existing.stream()
                    .min(Comparator.comparing(RefreshToken::getCreatedAt))  // Find oldest token and Delete it
                    .ifPresent(tokenRepository::delete);  
        }

        // Create new refresh token entity
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());  // Generate unique token string
        token.setUser(user);  
        token.setExpiryDate(Instant.now().plus(7, ChronoUnit.DAYS));  // 7-day validity
        token.setCreatedAt(Instant.now());  
        token.setIpAddress(ipAddress);  // Store client IP for security auditing
        
        // Ensure userAgent is never null (satisfies database constraints)
        token.setUserAgent((userAgent == null || userAgent.isBlank()) ? "unknown" : userAgent);

        // Log token creation (excluding sensitive token value in production)
        logger.debug(
                "Saving refresh token. token={}, userId={}, ipAddress={}, userAgent={}",
                token.getToken(),
                user != null ? user.getId() : null,
                token.getIpAddress(),
                token.getUserAgent());

        try {
            // saveAndFlush() forces immediate database write (not deferred)
            // Helps catch constraint violations early rather than at transaction commit
            RefreshToken saved = tokenRepository.saveAndFlush(token);
            logger.debug("Refresh token saved. id={}, token={}", saved.getId(), saved.getToken());
            return saved;
        } catch (RuntimeException ex) {
            // Log detailed error but don't expose database details to client
            logger.error("Failed to persist refresh token. token={}", token.getToken(), ex);
            throw ex;  // Re-throw for controller to handle
        }
    }

    /**
     * Validates a refresh token string and updates its last-used timestamp.
     * Returns Optional.empty() if token is invalid, expired, or not found.
     */
    public Optional<RefreshToken> validateAndRefresh(String tokenString) {
        // Early validation: reject null or empty tokens
        if (tokenString == null || tokenString.isBlank()) {
            logger.debug("Refresh token validation failed: tokenString is blank");
            return Optional.empty();
        }

        // Security: normalize token string (trim whitespace that might come from JSON)
        String rawToken = tokenString;
        String normalizedToken = tokenString.trim();
        if (!rawToken.equals(normalizedToken)) {
            logger.debug(
                    "Refresh token had surrounding whitespace (rawLen={}, trimmedLen={})",
                    rawToken.length(),
                    normalizedToken.length());
        }

        logger.debug("Validating refresh token (len={})", normalizedToken.length());

        // Look up token in database
        Optional<RefreshToken> found = tokenRepository.findByToken(normalizedToken);
        if (found.isEmpty()) {
            // Security: don't log actual token value in production
            logger.warn("Refresh token not found in DB (len={})", normalizedToken.length());
            return Optional.empty();
        }

        // Validate token using Optional functional programming pattern
        return found
                .filter(token -> {
                    // Check if token has expired
                    boolean ok = !token.isExpired();
                    if (!ok) {
                        logger.debug("Refresh token expired: token={}, expiryDate={}", 
                                token.getToken(), token.getExpiryDate());
                    }
                    return ok;
                })
                .map(token -> {
                    // Update last-used timestamp and persist
                    token.setLastUsedAt(Instant.now());
                    return tokenRepository.save(token);  // Save updated timestamp
                });
    }

    /**
     * Revokes (deletes) a specific token by its string value.
     * Used when user logs out or token is compromised.
     */
    public void revokeToken(String tokenString) {
        tokenRepository.findByToken(tokenString)
                .ifPresent(tokenRepository::delete);  // Delete if found
    }

    /**
     * Revokes ALL tokens for a specific user.
     * Used when user changes password or account is compromised.
     */
    public void revokeAllUserTokens(UUID userId) {
        tokenRepository.deleteAllByUserId(userId);  // Batch delete
    }

    /**
     * Scheduled job: runs daily at 2:00 AM to clean up expired tokens.
     * Prevents database bloat from expired tokens.
     */
    @Scheduled(cron = "0 0 2 * * *")  // Cron expression: seconds minutes hours day month weekday
    public void deleteExpiredTokens() {
        // Delete all tokens with expiry date before current time
        tokenRepository.deleteByExpiryDateBefore(Instant.now());
    }
}