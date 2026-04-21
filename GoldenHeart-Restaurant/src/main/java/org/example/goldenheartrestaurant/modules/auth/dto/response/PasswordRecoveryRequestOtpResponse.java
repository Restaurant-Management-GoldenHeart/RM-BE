package org.example.goldenheartrestaurant.modules.auth.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.goldenheartrestaurant.modules.auth.entity.PasswordRecoveryChannel;

import java.time.Instant;

@Getter
@Builder
public class PasswordRecoveryRequestOtpResponse {

    private PasswordRecoveryChannel channel;

    private String maskedDestination;

    private Instant expiresAt;

    private Instant resendAvailableAt;
}
