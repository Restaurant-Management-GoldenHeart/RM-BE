package org.example.goldenheartrestaurant.modules.auth.service;

import java.time.Instant;

public record IssuedTokens(
        String accessToken,
        String refreshToken,
        Instant expiresAt,
        String username,
        String role
) {
}
