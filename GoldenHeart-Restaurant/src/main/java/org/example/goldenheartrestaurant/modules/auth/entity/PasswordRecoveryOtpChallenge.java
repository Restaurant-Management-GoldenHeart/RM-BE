package org.example.goldenheartrestaurant.modules.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.goldenheartrestaurant.modules.identity.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "password_recovery_otp_challenges",
        indexes = {
                @Index(name = "idx_pw_recovery_otp_user_channel", columnList = "user_id,channel,purpose,created_at"),
                @Index(name = "idx_pw_recovery_otp_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Mỗi bản ghi đại diện cho 1 OTP đã phát hành cho nghiệp vụ quên mật khẩu.
 *
 * Thiết kế này cố tình tách riêng khỏi bảng users/refresh_tokens để:
 * - không trộn ý nghĩa "phiên đăng nhập" với "phiên khôi phục mật khẩu"
 * - audit được lịch sử gửi OTP
 * - dễ revoke / khóa OTP mà không ảnh hưởng session auth bình thường
 */
public class PasswordRecoveryOtpChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PasswordRecoveryPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PasswordRecoveryChannel channel;

    /**
     * Lưu snapshot contact tại thời điểm phát OTP để dễ audit khi profile bị đổi sau đó.
     */
    @Column(name = "destination_value", nullable = false, length = 100)
    private String destinationValue;

    /**
     * OTP là không gian nhỏ nên cũng phải hash bằng BCrypt thay vì hash nhanh kiểu SHA-256.
     */
    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "request_ip", length = 45)
    private String requestIp;

    @Column(name = "request_user_agent", length = 500)
    private String requestUserAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
