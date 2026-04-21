package org.example.goldenheartrestaurant.modules.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.config.PasswordRecoveryProperties;
import org.example.goldenheartrestaurant.common.exception.BadRequestException;
import org.example.goldenheartrestaurant.modules.auth.dto.request.PasswordRecoveryRequestOtpRequest;
import org.example.goldenheartrestaurant.modules.auth.dto.request.PasswordRecoveryResetPasswordRequest;
import org.example.goldenheartrestaurant.modules.auth.dto.request.PasswordRecoveryVerifyOtpRequest;
import org.example.goldenheartrestaurant.modules.auth.dto.response.PasswordRecoveryRequestOtpResponse;
import org.example.goldenheartrestaurant.modules.auth.dto.response.PasswordRecoveryVerifyOtpResponse;
import org.example.goldenheartrestaurant.modules.auth.entity.PasswordRecoveryChannel;
import org.example.goldenheartrestaurant.modules.auth.entity.PasswordRecoveryOtpChallenge;
import org.example.goldenheartrestaurant.modules.auth.entity.PasswordRecoveryPurpose;
import org.example.goldenheartrestaurant.modules.auth.entity.PasswordResetSession;
import org.example.goldenheartrestaurant.modules.auth.repository.PasswordRecoveryOtpChallengeRepository;
import org.example.goldenheartrestaurant.modules.auth.repository.PasswordResetSessionRepository;
import org.example.goldenheartrestaurant.modules.auth.repository.RefreshTokenRepository;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.entity.UserStatus;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Set;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
/**
 * Service xử lý toàn bộ flow quên mật khẩu bằng OTP.
 *
 * Flow chuẩn product:
 * 1. request OTP bằng email hoặc số điện thoại
 * 2. verify OTP thành công để lấy resetToken ngắn hạn
 * 3. dùng resetToken đổi mật khẩu mới
 * 4. revoke toàn bộ refresh token đang sống sau khi đổi mật khẩu
 */
public class PasswordRecoveryService {

    private static final PasswordRecoveryPurpose PASSWORD_RESET_PURPOSE = PasswordRecoveryPurpose.PASSWORD_RESET;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\s().-]{8,20}$");
    private static final String INVALID_OTP_MESSAGE = "OTP khong hop le hoac da het han";
    private static final String INVALID_RESET_SESSION_MESSAGE = "Phien dat lai mat khau khong hop le hoac da het han";
    /**
     * Theo product hiện tại, chỉ các tài khoản nhân sự vận hành mới được dùng flow quên mật khẩu.
     * Customer CRM record không đi qua auth recovery này.
     */
    private static final Set<String> PASSWORD_RECOVERY_ALLOWED_ROLES = Set.of("MANAGER", "STAFF", "KITCHEN");
    /**
     * Hash BCrypt hợp lệ dùng để burn thời gian CPU khi identifier không tồn tại.
     */
    private static final String DUMMY_BCRYPT_HASH = "$2a$12$K3L/Dh2mBJy4GpVfEw.9WOBGJzxI8rFhEiGP/wCKH9L3MidG9oqAS";

    private final UserRepository userRepository;
    private final PasswordRecoveryOtpChallengeRepository passwordRecoveryOtpChallengeRepository;
    private final PasswordResetSessionRepository passwordResetSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordRecoveryProperties passwordRecoveryProperties;
    private final PasswordRecoveryNotificationService passwordRecoveryNotificationService;

    @Transactional
    public PasswordRecoveryRequestOtpResponse requestOtp(
            PasswordRecoveryRequestOtpRequest request,
            String requestIp,
            String requestUserAgent
    ) {
        PasswordRecoveryChannel channel = request.getChannel();
        String normalizedIdentifier = normalizeIdentifier(channel, request.getIdentifier());
        LocalDateTime now = LocalDateTime.now();

        Optional<User> optionalUser = findActiveUserByRecoveryChannel(channel, normalizedIdentifier);
        if (optionalUser.isEmpty()) {
            performDummyHashWork(normalizedIdentifier);
            return buildOtpRequestResponse(channel, normalizedIdentifier, now.plus(passwordRecoveryProperties.getOtpExpiration()), now.plus(passwordRecoveryProperties.getResendCooldown()));
        }

        User user = optionalUser.get();
        if (isRateLimited(user.getId(), now)) {
            return buildOtpRequestResponse(channel, normalizedIdentifier, now.plus(passwordRecoveryProperties.getOtpExpiration()), now.plus(passwordRecoveryProperties.getResendCooldown()));
        }

        Optional<PasswordRecoveryOtpChallenge> latestChallenge = passwordRecoveryOtpChallengeRepository
                .findTopByUser_IdAndPurposeAndChannelAndVerifiedAtIsNullAndRevokedAtIsNullOrderByCreatedAtDesc(
                        user.getId(),
                        PASSWORD_RESET_PURPOSE,
                        channel
                );

        if (latestChallenge.isPresent()
                && latestChallenge.get().getCreatedAt().plus(passwordRecoveryProperties.getResendCooldown()).isAfter(now)) {
            PasswordRecoveryOtpChallenge challenge = latestChallenge.get();
            return buildOtpRequestResponse(channel, normalizedIdentifier, challenge.getExpiresAt(), challenge.getCreatedAt().plus(passwordRecoveryProperties.getResendCooldown()));
        }

        passwordRecoveryOtpChallengeRepository.revokeActiveChallenges(user.getId(), PASSWORD_RESET_PURPOSE, channel, now);

        String otp = generateOtp();
        PasswordRecoveryOtpChallenge challenge = passwordRecoveryOtpChallengeRepository.save(
                PasswordRecoveryOtpChallenge.builder()
                        .user(user)
                        .purpose(PASSWORD_RESET_PURPOSE)
                        .channel(channel)
                        .destinationValue(normalizedIdentifier)
                        .otpHash(passwordEncoder.encode(otp))
                        .expiresAt(now.plus(passwordRecoveryProperties.getOtpExpiration()))
                        .failedAttempts(0)
                        .requestIp(trimToLength(requestIp, 45))
                        .requestUserAgent(trimToLength(requestUserAgent, 500))
                        .build()
        );

        passwordRecoveryNotificationService.sendOtp(channel, normalizedIdentifier, otp, challenge.getExpiresAt());

        return buildOtpRequestResponse(
                channel,
                normalizedIdentifier,
                challenge.getExpiresAt(),
                now.plus(passwordRecoveryProperties.getResendCooldown())
        );
    }

    @Transactional
    public PasswordRecoveryVerifyOtpResponse verifyOtp(
            PasswordRecoveryVerifyOtpRequest request,
            String requestIp,
            String requestUserAgent
    ) {
        PasswordRecoveryChannel channel = request.getChannel();
        String normalizedIdentifier = normalizeIdentifier(channel, request.getIdentifier());
        LocalDateTime now = LocalDateTime.now();

        if (request.getOtp().length() != passwordRecoveryProperties.getOtpLength()) {
            throw new BadRequestException(INVALID_OTP_MESSAGE);
        }

        Optional<User> optionalUser = findActiveUserByRecoveryChannel(channel, normalizedIdentifier);
        if (optionalUser.isEmpty()) {
            performDummyHashWork(request.getOtp());
            throw new BadRequestException(INVALID_OTP_MESSAGE);
        }

        User user = optionalUser.get();
        PasswordRecoveryOtpChallenge challenge = passwordRecoveryOtpChallengeRepository
                .findTopByUser_IdAndPurposeAndChannelAndVerifiedAtIsNullAndRevokedAtIsNullOrderByCreatedAtDesc(
                        user.getId(),
                        PASSWORD_RESET_PURPOSE,
                        channel
                )
                .orElseThrow(() -> new BadRequestException(INVALID_OTP_MESSAGE));

        if (challenge.getExpiresAt().isBefore(now)) {
            challenge.setRevokedAt(now);
            passwordRecoveryOtpChallengeRepository.save(challenge);
            throw new BadRequestException(INVALID_OTP_MESSAGE);
        }

        if (!passwordEncoder.matches(request.getOtp(), challenge.getOtpHash())) {
            challenge.setFailedAttempts(challenge.getFailedAttempts() + 1);
            if (challenge.getFailedAttempts() >= passwordRecoveryProperties.getMaxVerifyAttempts()) {
                challenge.setRevokedAt(now);
            }
            passwordRecoveryOtpChallengeRepository.save(challenge);
            throw new BadRequestException(INVALID_OTP_MESSAGE);
        }

        challenge.setVerifiedAt(now);
        passwordRecoveryOtpChallengeRepository.save(challenge);
        passwordRecoveryOtpChallengeRepository.revokeAllActiveChallengesByPurpose(user.getId(), PASSWORD_RESET_PURPOSE, now);
        passwordResetSessionRepository.revokeAllActiveSessionsByUserId(user.getId(), now);

        String rawResetToken = generateResetToken();
        PasswordResetSession resetSession = passwordResetSessionRepository.save(
                PasswordResetSession.builder()
                        .user(user)
                        .challenge(challenge)
                        .resetTokenHash(hashToken(rawResetToken))
                        .expiresAt(now.plus(passwordRecoveryProperties.getResetSessionExpiration()))
                        .requestIp(trimToLength(requestIp, 45))
                        .requestUserAgent(trimToLength(requestUserAgent, 500))
                        .build()
        );

        return PasswordRecoveryVerifyOtpResponse.builder()
                .resetToken(rawResetToken)
                .expiresAt(toInstant(resetSession.getExpiresAt()))
                .build();
    }

    @Transactional
    public void resetPassword(PasswordRecoveryResetPasswordRequest request) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetSession resetSession = passwordResetSessionRepository.findByResetTokenHash(hashToken(request.getResetToken()))
                .orElseThrow(() -> new BadRequestException(INVALID_RESET_SESSION_MESSAGE));

        if (resetSession.getUsedAt() != null
                || resetSession.getRevokedAt() != null
                || resetSession.getExpiresAt().isBefore(now)) {
            if (resetSession.getRevokedAt() == null && resetSession.getUsedAt() == null && resetSession.getExpiresAt().isBefore(now)) {
                resetSession.setRevokedAt(now);
                passwordResetSessionRepository.save(resetSession);
            }
            throw new BadRequestException(INVALID_RESET_SESSION_MESSAGE);
        }

        User user = userRepository.findActiveAuthUserById(resetSession.getUser().getId(), UserStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException(INVALID_RESET_SESSION_MESSAGE));
        if (!isPasswordRecoveryEligible(user)) {
            throw new BadRequestException(INVALID_RESET_SESSION_MESSAGE);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetSession.setUsedAt(now);
        passwordResetSessionRepository.save(resetSession);
        passwordResetSessionRepository.revokeAllActiveSessionsByUserId(user.getId(), now);
        passwordRecoveryOtpChallengeRepository.revokeAllActiveChallengesByPurpose(user.getId(), PASSWORD_RESET_PURPOSE, now);
        refreshTokenRepository.revokeAllActiveByUserId(user.getId(), now);
    }

    private Optional<User> findActiveUserByRecoveryChannel(PasswordRecoveryChannel channel, String identifier) {
        Optional<User> optionalUser;
        if (channel == PasswordRecoveryChannel.EMAIL) {
            optionalUser = userRepository.findActiveAuthUserByEmail(identifier, UserStatus.ACTIVE);
        } else {
            optionalUser = userRepository.findActiveAuthUserByPhone(identifier, UserStatus.ACTIVE);
        }

        // Chặn customer/admin ra khỏi luồng recovery nếu product không cho phép.
        return optionalUser.filter(this::isPasswordRecoveryEligible);
    }

    private boolean isRateLimited(Integer userId, LocalDateTime now) {
        LocalDateTime windowStart = now.minus(passwordRecoveryProperties.getRequestLimitWindow());
        long totalRecentRequests = passwordRecoveryOtpChallengeRepository.countByUser_IdAndPurposeAndCreatedAtAfter(
                userId,
                PASSWORD_RESET_PURPOSE,
                windowStart
        );
        return totalRecentRequests >= passwordRecoveryProperties.getRequestLimitPerWindow();
    }

    private PasswordRecoveryRequestOtpResponse buildOtpRequestResponse(
            PasswordRecoveryChannel channel,
            String destination,
            LocalDateTime expiresAt,
            LocalDateTime resendAvailableAt
    ) {
        return PasswordRecoveryRequestOtpResponse.builder()
                .channel(channel)
                .maskedDestination(maskDestination(channel, destination))
                .expiresAt(toInstant(expiresAt))
                .resendAvailableAt(toInstant(resendAvailableAt))
                .build();
    }

    private String normalizeIdentifier(PasswordRecoveryChannel channel, String identifier) {
        String normalized = identifier == null ? null : identifier.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new BadRequestException("Identifier is required");
        }

        if (channel == PasswordRecoveryChannel.EMAIL) {
            normalized = normalized.toLowerCase();
            if (!EMAIL_PATTERN.matcher(normalized).matches()) {
                throw new BadRequestException("Email khong hop le");
            }
            return normalized;
        }

        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException("So dien thoai khong hop le");
        }
        return normalized;
    }

    private String generateOtp() {
        int otpLength = passwordRecoveryProperties.getOtpLength();
        int min = (int) Math.pow(10, otpLength - 1);
        int maxExclusive = (int) Math.pow(10, otpLength);
        return String.valueOf(min + SECURE_RANDOM.nextInt(maxExclusive - min));
    }

    private String generateResetToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();

            for (byte value : digest) {
                builder.append(String.format("%02x", value));
            }

            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String maskDestination(PasswordRecoveryChannel channel, String destination) {
        if (channel == PasswordRecoveryChannel.EMAIL) {
            int atIndex = destination.indexOf('@');
            if (atIndex <= 1) {
                return "***" + destination.substring(Math.max(atIndex, 0));
            }

            String localPart = destination.substring(0, atIndex);
            String domain = destination.substring(atIndex);
            return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domain;
        }

        if (destination.length() <= 4) {
            return "****";
        }

        return "*".repeat(Math.max(1, destination.length() - 4)) + destination.substring(destination.length() - 4);
    }

    private Instant toInstant(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private void performDummyHashWork(String value) {
        passwordEncoder.matches(value == null ? "000000" : value, DUMMY_BCRYPT_HASH);
    }

    private boolean isPasswordRecoveryEligible(User user) {
        if (user.getRole() == null || user.getRole().getName() == null) {
            return false;
        }

        return PASSWORD_RECOVERY_ALLOWED_ROLES.contains(user.getRole().getName().toUpperCase());
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
