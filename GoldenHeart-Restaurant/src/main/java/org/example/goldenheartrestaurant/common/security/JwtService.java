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

/**
 * Service chịu trách nhiệm xử lý toàn bộ JWT trong hệ thống.
 *
 * Những gì lớp này làm:
 * - tạo access token
 * - tạo refresh token
 * - parse và verify token
 * - dựng Authentication cho Spring Security
 * - build hoặc xóa cookie chứa refresh token
 *
 * Hệ thống tách rõ 2 loại token bằng claim tokenType:
 * - access: dùng để gọi API protected
 * - refresh: chỉ dùng để làm mới phiên đăng nhập
 *
 * Việc tách riêng này giúp tránh lỗi dùng nhầm refresh token như access token
 * hoặc ngược lại.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String ROLE_CLAIM = "role";

    private final JwtProperties jwtProperties;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Access token có tuổi thọ ngắn để giảm rủi ro nếu token bị lộ.
     * Client sẽ gửi token này trong header Authorization cho các API cần xác thực.
     */
    public String generateAccessToken(CustomUserDetails userDetails) {
        return buildToken(userDetails, ACCESS_TOKEN_TYPE, jwtProperties.getAccessTokenExpiration());
    }

    /**
     * Refresh token sống lâu hơn access token.
     * Trong dự án này, refresh token được đặt trong HttpOnly cookie để JavaScript
     * phía trình duyệt không đọc được trực tiếp.
     */
    public String generateRefreshToken(CustomUserDetails userDetails) {
        return buildToken(userDetails, REFRESH_TOKEN_TYPE, jwtProperties.getRefreshTokenExpiration());
    }

    public Instant getAccessTokenExpiry() {
        return Instant.now().plus(jwtProperties.getAccessTokenExpiration());
    }

    public ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        // Toàn bộ flag của cookie lấy từ config để tiện đổi theo từng môi trường.
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), refreshToken)
                .httpOnly(jwtProperties.isRefreshCookieHttpOnly())
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path(jwtProperties.getRefreshCookiePath())
                .maxAge(jwtProperties.getRefreshTokenExpiration())
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        // Logout cần xóa cookie ở trình duyệt.
        // Việc revoke token trong DB sẽ do tầng service auth xử lý riêng.
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), "")
                .httpOnly(jwtProperties.isRefreshCookieHttpOnly())
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path(jwtProperties.getRefreshCookiePath())
                .maxAge(Duration.ZERO)
                .build();
    }

    /**
     * Dựng Authentication từ access token.
     *
     * Đây là phương thức cốt lõi được JwtAuthenticationFilter gọi ở mọi request protected.
     * Luồng xử lý:
     * 1. parse token và kiểm tra chữ ký
     * 2. xác nhận tokenType đúng là access
     * 3. load lại user mới nhất từ DB
     * 4. dựng Authentication để nhét vào SecurityContext
     *
     * Điểm quan trọng:
     * Dù token có chứa role và userId, hệ thống vẫn load user lại từ DB để bám trạng thái mới nhất.
     * Nhờ vậy nếu user bị đổi quyền, khóa tài khoản hoặc bị xóa mềm thì request kế tiếp phản ánh ngay.
     */
    public Authentication buildAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken, ACCESS_TOKEN_TYPE);

        CustomUserDetails userDetails =
                (CustomUserDetails) userDetailsService.loadUserByUsername(claims.getSubject());

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
                // tokenType dùng để chặn việc dùng sai loại token ở sai ngữ cảnh.
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                // Các claim này hữu ích cho client, log và debug.
                // Tuy nhiên quyền thực tế vẫn luôn được load lại từ DB khi xác thực request.
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
            throw new JwtException("Invalid token type");
        }

        return claims;
    }

    private SecretKey getSigningKey() {
        byte[] secretBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);

        // HMAC SHA key quá ngắn sẽ làm suy yếu đáng kể độ an toàn của JWT.
        // Với secret dưới 32 byte, hệ thống chủ động fail sớm thay vì chạy âm thầm.
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 characters");
        }

        return Keys.hmacShaKeyFor(secretBytes);
    }
}
