package org.example.goldenheartrestaurant.common.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.password-recovery")
/**
 * Gom toàn bộ cấu hình cho flow quên mật khẩu dùng OTP.
 *
 * Tách riêng nhóm cấu hình này giúp:
 * - đổi TTL / cooldown theo từng môi trường
 * - bật log OTP ở local nhưng tắt ở production
 * - không hard-code rule bảo mật trong service
 */
public class PasswordRecoveryProperties {

    @Min(4)
    @Max(8)
    private int otpLength = 6;

    private Duration otpExpiration = Duration.ofMinutes(5);

    private Duration resetSessionExpiration = Duration.ofMinutes(15);

    private Duration resendCooldown = Duration.ofSeconds(60);

    private Duration requestLimitWindow = Duration.ofMinutes(15);

    @Min(1)
    @Max(20)
    private int requestLimitPerWindow = 5;

    @Min(1)
    @Max(10)
    private int maxVerifyAttempts = 5;

    /**
     * Chỉ nên bật ở local/dev để test nhanh khi chưa có SMTP hay SMS provider thật.
     */
    private boolean devLogDelivery = true;

    @NotBlank
    private String emailFrom = "noreply@goldenheart.com";
}
