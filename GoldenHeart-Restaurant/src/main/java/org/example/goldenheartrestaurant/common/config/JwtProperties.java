package org.example.goldenheartrestaurant.common.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
/**
 * Nơi gom toàn bộ cấu hình liên quan đến JWT và refresh-token cookie.
 *
 * Ý nghĩa:
 * - tách cấu hình ra khỏi code nghiệp vụ
 * - dễ thay đổi theo từng môi trường local / staging / production
 * - tránh hard-code secret, issuer, thời gian sống của token ngay trong service
 */
public class JwtProperties {

    @NotBlank
    private String secret;

    @NotBlank
    private String issuer;

    private Duration accessTokenExpiration = Duration.ofMinutes(15);

    private Duration refreshTokenExpiration = Duration.ofDays(7);

    private String refreshCookieName = "refreshToken";

    private boolean refreshCookieSecure = false;

    private boolean refreshCookieHttpOnly = true;

    private String refreshCookieSameSite = "Strict";

    // Chỉ cho cookie refresh đi vào nhóm endpoint auth để thu hẹp phạm vi gửi cookie.
    private String refreshCookiePath = "/api/v1/auth";
}
