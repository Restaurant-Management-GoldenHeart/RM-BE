package org.example.goldenheartrestaurant.modules.auth.service;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.modules.auth.entity.RefreshToken;
import org.example.goldenheartrestaurant.modules.auth.repository.RefreshTokenRepository;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
/**
 * Service quản lý vòng đời refresh token ở phía server.
 *
 * Ý nghĩa:
 * - không chỉ tin vào JWT tự chứa
 * - cho phép revoke token
 * - cho phép rotation khi refresh
 * - hỗ trợ logout thật sự
 */
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Lưu refresh token dưới dạng hash.
     *
     * Backend không lưu raw token để giảm thiểu rủi ro nếu database bị lộ.
     */
    @Transactional
    public RefreshToken store(User user, String rawRefreshToken, LocalDateTime expiresAt) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawRefreshToken))
                .expiresAt(expiresAt)
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Tìm token trong DB và đảm bảo:
     * - token có tồn tại
     * - chưa bị revoke
     * - chưa hết hạn ở tầng DB
     * - đúng chủ sở hữu
     */
    @Transactional(readOnly = true)
    public RefreshToken requireActiveToken(String rawRefreshToken, String expectedUsername) {
        // Chữ ký JWT đúng là chưa đủ.
        // Token còn phải tồn tại và còn hiệu lực trong DB.
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
                .orElseThrow(() -> new JwtException("Refresh token is not recognized"));

        if (storedToken.isRevoked()) {
            throw new JwtException("Refresh token has been revoked");
        }

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new JwtException("Refresh token has expired");
        }

        if (!storedToken.getUser().getUsername().equalsIgnoreCase(expectedUsername)) {
            throw new JwtException("Refresh token does not belong to the current user");
        }

        return storedToken;
    }

    @Transactional
    public void revoke(RefreshToken refreshToken) {
        if (!refreshToken.isRevoked()) {
            // Lưu thêm thời điểm revoke / last used để sau này audit session dễ hơn.
            refreshToken.setRevoked(true);
            refreshToken.setRevokedAt(LocalDateTime.now());
            refreshToken.setLastUsedAt(LocalDateTime.now());
            refreshTokenRepository.save(refreshToken);
        }
    }

    @Transactional
    public void revokeByRawToken(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
                .ifPresent(this::revoke);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();

            for (byte value : digest) {
                // Chuyển mảng byte sang chuỗi hex để lưu DB.
                builder.append(String.format("%02x", value));
            }

            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            // SHA-256 gần như luôn có; nếu lỗi ở đây thì là lỗi môi trường runtime.
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
