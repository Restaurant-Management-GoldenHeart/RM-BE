package org.example.goldenheartrestaurant.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordRecoveryResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    @Size(max = 255, message = "Reset token must be at most 255 characters")
    private String resetToken;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Password must contain at least 1 uppercase letter, 1 lowercase letter, and 1 number"
    )
    private String newPassword;
}
