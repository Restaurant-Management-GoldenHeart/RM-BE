package org.example.goldenheartrestaurant.modules.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.config.JwtProperties;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.security.JwtService;
import org.example.goldenheartrestaurant.modules.auth.dto.request.LoginRequest;
import org.example.goldenheartrestaurant.modules.auth.dto.request.RegisterRequest;
import org.example.goldenheartrestaurant.modules.auth.dto.response.AuthResponse;
import org.example.goldenheartrestaurant.modules.auth.dto.response.RegisterResponse;
import org.example.goldenheartrestaurant.modules.auth.service.AuthService;
import org.example.goldenheartrestaurant.modules.auth.service.IssuedTokens;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
/**
 * Public authentication API for register, login, refresh and logout.
 */
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<RegisterResponse>builder()
                        .message("Register successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        IssuedTokens issuedTokens = authService.login(request);
        return buildTokenResponse("Login successfully", issuedTokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(HttpServletRequest request) {
        // Refresh token is intentionally read from cookie rather than request body.
        String refreshToken = readCookie(request, jwtProperties.getRefreshCookieName());
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token cookie is missing");
        }

        IssuedTokens issuedTokens = authService.refresh(refreshToken);
        return buildTokenResponse("Refresh token successfully", issuedTokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String refreshToken = readCookie(request, jwtProperties.getRefreshCookieName());
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtService.clearRefreshTokenCookie().toString())
                .body(ApiResponse.<Void>builder()
                        .message("Logout successfully")
                        .build());
    }

    private ResponseEntity<ApiResponse<AuthResponse>> buildTokenResponse(String message, IssuedTokens issuedTokens) {
        // Only access token is returned in JSON. Refresh token is written to Set-Cookie.
        AuthResponse response = AuthResponse.builder()
                .accessToken(issuedTokens.accessToken())
                .tokenType("Bearer")
                .expiresAt(issuedTokens.expiresAt())
                .username(issuedTokens.username())
                .role(issuedTokens.role())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtService.buildRefreshTokenCookie(issuedTokens.refreshToken()).toString())
                .body(ApiResponse.<AuthResponse>builder()
                        .message(message)
                        .data(response)
                        .build());
    }

    private String readCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
