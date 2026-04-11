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
 * Service trung tâm xử lý JWT.
 *
 * Trách nhiệm chính:
 * - tạo access token
 * - tạo refresh token
 * - parse và verify token
 * - dựng Authentication cho Spring Security
 * - build / clear cookie cho refresh token
 *
 * Dự án dùng thêm claim tokenType để tách rõ:
 * - access token chỉ dùng cho request protected
 * - refresh token chỉ dùng cho refresh session
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
     * Frontend sẽ gửi token này trong Authorization header cho các API protected.
     */
    public String generateAccessToken(CustomUserDetails userDetails) {
        return buildToken(userDetails, ACCESS_TOKEN_TYPE, jwtProperties.getAccessTokenExpiration());
    }

    /**
     * Refresh token sống lâu hơn và chỉ nên đặt trong HttpOnly cookie.
     * Làm vậy để JavaScript phía frontend không đọc trực tiếp được token nhạy cảm này.
     */
    public String generateRefreshToken(CustomUserDetails userDetails) {
        return buildToken(userDetails, REFRESH_TOKEN_TYPE, jwtProperties.getRefreshTokenExpiration());
    }

    public Instant getAccessTokenExpiry() {
        return Instant.now().plus(jwtProperties.getAccessTokenExpiration());
    }

    public ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        // Toàn bộ flag của cookie đọc từ config để dễ đổi theo môi trường triển khai.
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), refreshToken)
                .httpOnly(jwtProperties.isRefreshCookieHttpOnly())
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path(jwtProperties.getRefreshCookiePath())
                .maxAge(jwtProperties.getRefreshTokenExpiration())
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        // Logout cần xóa cookie ở phía client.
        // Việc revoke token trong DB sẽ được AuthService / RefreshTokenService xử lý riêng.
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), "")
                .httpOnly(jwtProperties.isRefreshCookieHttpOnly())
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path(jwtProperties.getRefreshCookiePath())
                .maxAge(Duration.ZERO)
                .build();
    }

    /**
     * Method này được JwtAuthenticationFilter gọi ở mọi request protected.
     *
     * Luồng xử lý:
     * 1. parse access token
     * 2. kiểm tra token type
     * 3. load user mới nhất từ DB
     * 4. dựng Authentication để nhét vào SecurityContext
     */
    public Authentication buildAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken, ACCESS_TOKEN_TYPE);

        // Chủ ý load lại user từ DB thay vì tin hoàn toàn vào claim trong token.
        // Nhờ đó nếu:
        // - role bị đổi
        // - user bị khóa
        // - user bị soft-delete
        // thì request kế tiếp sẽ phản ánh ngay trạng thái mới.
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
                // Dùng claim này để chặn việc lấy refresh token giả làm access token và ngược lại.
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                // Các claim này hữu ích cho client / log / debug,
                // nhưng quyền thật vẫn được load lại từ DB khi xác thực request.
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
            // Chặn việc dùng sai loại token ở sai ngữ cảnh.
            throw new JwtException("Invalid token type");
        }

        return claims;
    }

    private SecretKey getSigningKey() {
        byte[] secretBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < 32) {
            // Secret quá ngắn sẽ làm khóa HMAC yếu và tăng rủi ro bảo mật.
            throw new IllegalArgumentException("JWT secret must contain at least 32 characters");
        }

        return Keys.hmacShaKeyFor(secretBytes);
    }
}
