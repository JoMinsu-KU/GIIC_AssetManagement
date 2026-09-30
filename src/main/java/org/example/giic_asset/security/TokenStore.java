package org.example.giic_asset.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {

    private final Map<String, Long> activeRefreshTokens = new ConcurrentHashMap<>();
    private final Map<String, Long> revokedAccessTokens = new ConcurrentHashMap<>();

    public void storeRefreshToken(String token, long expiresAtMillis) {
        cleanupExpired();
        activeRefreshTokens.put(token, expiresAtMillis);
    }

    public boolean isRefreshTokenActive(String token) {
        cleanupExpired();
        Long expiresAt = activeRefreshTokens.get(token);
        return expiresAt != null && expiresAt > now();
    }

    public void revokeRefreshToken(String token) {
        if (token != null) {
            activeRefreshTokens.remove(token);
        }
    }

    public void revokeAccessToken(String token, long expiresAtMillis) {
        if (token != null && expiresAtMillis > now()) {
            cleanupExpired();
            revokedAccessTokens.put(token, expiresAtMillis);
        }
    }

    public boolean isAccessTokenRevoked(String token) {
        cleanupExpired();
        Long expiresAt = revokedAccessTokens.get(token);
        return expiresAt != null && expiresAt > now();
    }

    private void cleanupExpired() {
        long now = now();
        activeRefreshTokens.entrySet().removeIf(entry -> entry.getValue() <= now);
        revokedAccessTokens.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private long now() {
        return Instant.now().toEpochMilli();
    }
}
