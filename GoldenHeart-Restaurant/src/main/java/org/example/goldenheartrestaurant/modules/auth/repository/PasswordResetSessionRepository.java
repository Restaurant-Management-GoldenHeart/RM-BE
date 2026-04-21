package org.example.goldenheartrestaurant.modules.auth.repository;

import org.example.goldenheartrestaurant.modules.auth.entity.PasswordResetSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetSessionRepository extends JpaRepository<PasswordResetSession, Integer> {

    Optional<PasswordResetSession> findByResetTokenHash(String resetTokenHash);

    @Modifying
    @Query("""
            update PasswordResetSession s
            set s.revokedAt = :revokedAt
            where s.user.id = :userId
              and s.usedAt is null
              and s.revokedAt is null
            """)
    void revokeAllActiveSessionsByUserId(
            @Param("userId") Integer userId,
            @Param("revokedAt") LocalDateTime revokedAt
    );
}
