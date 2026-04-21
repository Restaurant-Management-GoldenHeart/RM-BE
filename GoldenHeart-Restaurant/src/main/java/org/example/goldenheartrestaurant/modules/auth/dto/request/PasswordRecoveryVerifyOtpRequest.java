package org.example.goldenheartrestaurant.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.example.goldenheartrestaurant.modules.auth.entity.PasswordRecoveryChannel;

@Getter
@Setter
public class PasswordRecoveryVerifyOtpRequest {

    @NotNull(message = "Recovery channel is required")
    private PasswordRecoveryChannel channel;

    @NotBlank(message = "Identifier is required")
    @Size(max = 100, message = "Identifier must be at most 100 characters")
    private String identifier;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{4,8}$", message = "OTP must be between 4 and 8 digits")
    private String otp;
}
