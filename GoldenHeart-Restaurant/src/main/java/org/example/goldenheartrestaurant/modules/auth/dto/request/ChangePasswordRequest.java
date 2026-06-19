package org.example.goldenheartrestaurant.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @Size(max = 100, message = "Current password must be at most 100 characters")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Password must contain at least 1 uppercase letter, 1 lowercase letter, and 1 number"
    )
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    @Size(min = 8, max = 100, message = "Password confirmation must be between 8 and 100 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Password confirmation must contain at least 1 uppercase letter, 1 lowercase letter, and 1 number"
    )
    private String confirmNewPassword;
}
