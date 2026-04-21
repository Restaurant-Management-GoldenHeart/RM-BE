package org.example.goldenheartrestaurant.modules.auth.repository;

import org.example.goldenheartrestaurant.modules.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    // Tìm token theo bản hash để backend không cần lưu raw refresh token trong DB.
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update RefreshToken rt
            set rt.revoked = true,
                rt.revokedAt = :revokedAt,
                rt.lastUsedAt = coalesce(rt.lastUsedAt, :revokedAt)
            where rt.user.id = :userId
              and rt.revoked = false
            """)
    void revokeAllActiveByUserId(
            @Param("userId") Integer userId,
            @Param("revokedAt") LocalDateTime revokedAt
    );
}
