package org.example.goldenheartrestaurant.modules.auth.service;

import java.time.Instant;

public record IssuedTokens(
        String accessToken,
        String refreshToken,
        Instant expiresAt,
        String username,
        String role
) {
    // Record này là gói dữ liệu trung gian giữa service và controller.
    // Nó gom cả access token, refresh token và metadata để trả response gọn hơn.
}
