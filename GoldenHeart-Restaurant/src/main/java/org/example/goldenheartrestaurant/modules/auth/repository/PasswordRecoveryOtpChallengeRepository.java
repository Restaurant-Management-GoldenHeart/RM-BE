package org.example.goldenheartrestaurant.modules.auth.repository;

import org.example.goldenheartrestaurant.modules.auth.entity.PasswordRecoveryChannel;
import org.example.goldenheartrestaurant.modules.auth.entity.PasswordRecoveryOtpChallenge;
import org.example.goldenheartrestaurant.modules.auth.entity.PasswordRecoveryPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordRecoveryOtpChallengeRepository extends JpaRepository<PasswordRecoveryOtpChallenge, Integer> {

    Optional<PasswordRecoveryOtpChallenge> findTopByUser_IdAndPurposeAndChannelOrderByCreatedAtDesc(
            Integer userId,
            PasswordRecoveryPurpose purpose,
            PasswordRecoveryChannel channel
    );

    Optional<PasswordRecoveryOtpChallenge> findTopByUser_IdAndPurposeAndChannelAndVerifiedAtIsNullAndRevokedAtIsNullOrderByCreatedAtDesc(
            Integer userId,
            PasswordRecoveryPurpose purpose,
            PasswordRecoveryChannel channel
    );

    long countByUser_IdAndPurposeAndCreatedAtAfter(
            Integer userId,
            PasswordRecoveryPurpose purpose,
            LocalDateTime createdAfter
    );

    @Modifying
    @Query("""
            update PasswordRecoveryOtpChallenge c
            set c.revokedAt = :revokedAt
            where c.user.id = :userId
              and c.purpose = :purpose
              and c.channel = :channel
              and c.verifiedAt is null
              and c.revokedAt is null
            """)
    void revokeActiveChallenges(
            @Param("userId") Integer userId,
            @Param("purpose") PasswordRecoveryPurpose purpose,
            @Param("channel") PasswordRecoveryChannel channel,
            @Param("revokedAt") LocalDateTime revokedAt
    );

    @Modifying
    @Query("""
            update PasswordRecoveryOtpChallenge c
            set c.revokedAt = :revokedAt
            where c.user.id = :userId
              and c.purpose = :purpose
              and c.verifiedAt is null
              and c.revokedAt is null
            """)
    void revokeAllActiveChallengesByPurpose(
            @Param("userId") Integer userId,
            @Param("purpose") PasswordRecoveryPurpose purpose,
            @Param("revokedAt") LocalDateTime revokedAt
    );
}
