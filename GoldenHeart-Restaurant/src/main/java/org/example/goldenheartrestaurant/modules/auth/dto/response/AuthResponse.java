package org.example.goldenheartrestaurant.modules.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AuthResponse {

    private String accessToken;

    private String tokenType;

    private Instant expiresAt;

    private String username;

    private String role;
}
