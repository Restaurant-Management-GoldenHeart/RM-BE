package org.example.goldenheartrestaurant.modules.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RegisterResponse {

    private Integer userId;

    private String username;

    private String email;

    private String fullName;

    private String role;

    private LocalDateTime createdAt;
}
