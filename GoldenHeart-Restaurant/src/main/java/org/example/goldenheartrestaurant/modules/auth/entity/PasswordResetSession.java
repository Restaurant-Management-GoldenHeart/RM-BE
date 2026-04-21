package org.example.goldenheartrestaurant.modules.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "password_reset_sessions",
        indexes = {
                @Index(name = "idx_pw_reset_session_token_hash", columnList = "reset_token_hash", unique = true),
                @Index(name = "idx_pw_reset_session_user", columnList = "user_id,created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Phiên reset password ngắn hạn được cấp sau khi OTP đã xác thực thành công.
 *
 * Tách bước này khỏi OTP giúp flow an toàn hơn:
 * - OTP chỉ dùng để chứng minh người dùng có quyền tiếp tục
 * - resetToken entropy cao hơn, phù hợp cho bước đổi mật khẩu thật sự
 * - dễ đánh dấu single-use ở bước reset password
 */
public class PasswordResetSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private PasswordRecoveryOtpChallenge challenge;

    @Column(name = "reset_token_hash", nullable = false, unique = true, length = 128)
    private String resetTokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

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
