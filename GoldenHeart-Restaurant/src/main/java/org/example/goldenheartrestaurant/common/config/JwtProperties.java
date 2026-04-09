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

    private String refreshCookiePath = "/api/v1/auth";
}
