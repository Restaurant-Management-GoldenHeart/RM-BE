package org.example.goldenheartrestaurant.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.config.JwtProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
/**
 * Central place for JWT creation, parsing and refresh-cookie helpers.
 *
 * The project uses token typing so an access token and refresh token cannot be interchanged.
 */
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String ROLE_CLAIM = "role";

    private final JwtProperties jwtProperties;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Access token sống ngắn để giảm rủi ro nếu token bị lộ.
     * Client sẽ gửi token này ở Authorization header cho các request cần đăng nhập.
     */
    public String generateAccessToken(CustomUserDetails userDetails) {
        return buildToken(userDetails, ACCESS_TOKEN_TYPE, jwtProperties.getAccessTokenExpiration());
    }

    /**
     * Refresh token sống lâu hơn và chỉ nên lưu trong HttpOnly cookie.
     * Cookie HttpOnly giúp JavaScript ở frontend không đọc được token này.
     */
    public String generateRefreshToken(CustomUserDetails userDetails) {
        return buildToken(userDetails, REFRESH_TOKEN_TYPE, jwtProperties.getRefreshTokenExpiration());
    }

    public Instant getAccessTokenExpiry() {
        return Instant.now().plus(jwtProperties.getAccessTokenExpiration());
    }

    public ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        // Cookie flags come from config so environments can tune security without changing business logic.
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), refreshToken)
                .httpOnly(jwtProperties.isRefreshCookieHttpOnly())
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path(jwtProperties.getRefreshCookiePath())
                .maxAge(jwtProperties.getRefreshTokenExpiration())
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        // Logout clears the client-side cookie; DB revocation happens in RefreshTokenService/AuthService.
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), "")
                .httpOnly(jwtProperties.isRefreshCookieHttpOnly())
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path(jwtProperties.getRefreshCookiePath())
                .maxAge(Duration.ZERO)
                .build();
    }

    /**
     * Method này được JwtAuthenticationFilter gọi ở mọi request.
     * Nếu token hợp lệ, method sẽ dựng Authentication và đưa vào SecurityContext.
     */
    public Authentication buildAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken, ACCESS_TOKEN_TYPE);
        // We intentionally reload the current user from DB instead of trusting claims alone.
        // That makes role changes and soft-delete effective immediately on the next request.
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(claims.getSubject());

        return UsernamePasswordAuthenticationToken.authenticated(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    public String extractUsernameFromRefreshToken(String refreshToken) {
        return parseClaims(refreshToken, REFRESH_TOKEN_TYPE).getSubject();
    }

    public LocalDateTime extractRefreshTokenExpiry(String refreshToken) {
        Date expiration = parseClaims(refreshToken, REFRESH_TOKEN_TYPE).getExpiration();
        return LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
    }

    private String buildToken(CustomUserDetails userDetails, String tokenType, Duration ttl) {
        Instant now = Instant.now();
        Instant expiry = now.plus(ttl);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                // tokenType separates access-token and refresh-token responsibilities.
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                // These claims help clients/logging, but authorization still reloads server truth from DB.
                .claim(ROLE_CLAIM, userDetails.getRoleName())
                .claim("userId", userDetails.getUserId())
                .signWith(getSigningKey())
                .compact();
    }

    private Claims parseClaims(String token, String expectedType) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!expectedType.equals(tokenType)) {
            // Prevents refresh token from being accepted where access token is expected and vice versa.
            throw new JwtException("Invalid token type");
        }

        return claims;
    }

    private SecretKey getSigningKey() {
        byte[] secretBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < 32) {
            // HMAC JWT signing requires enough entropy; short secrets are a security risk.
            throw new IllegalArgumentException("JWT secret must contain at least 32 characters");
        }

        return Keys.hmacShaKeyFor(secretBytes);
    }
}
