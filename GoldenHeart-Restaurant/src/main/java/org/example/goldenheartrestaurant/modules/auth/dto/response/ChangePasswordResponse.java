package org.example.goldenheartrestaurant.modules.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChangePasswordResponse {

    private Integer userId;

    private String username;

    private String role;

    private LocalDateTime changedAt;

    private int revokedSessionCount;

    private boolean requiresLoginAgain;
}
