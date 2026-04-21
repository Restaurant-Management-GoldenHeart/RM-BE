package org.example.goldenheartrestaurant.modules.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class PasswordRecoveryVerifyOtpResponse {

    private String resetToken;

    private Instant expiresAt;
}
