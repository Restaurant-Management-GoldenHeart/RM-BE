package org.example.goldenheartrestaurant.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.goldenheartrestaurant.common.config.PasswordRecoveryProperties;
import org.example.goldenheartrestaurant.modules.auth.entity.PasswordRecoveryChannel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * Service gom việc gửi OTP cho các kênh recovery.
 *
 * Thiết kế theo hướng:
 * - EMAIL gửi thật khi có JavaMailSender / SMTP
 * - SMS để sẵn điểm mở rộng cho provider thật
 * - local/dev có thể fallback sang log để test end-to-end
 */
public class PasswordRecoveryNotificationService {

    private static final DateTimeFormatter OTP_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final PasswordRecoveryProperties passwordRecoveryProperties;

    public void sendOtp(PasswordRecoveryChannel channel, String destination, String otp, LocalDateTime expiresAt) {
        if (channel == PasswordRecoveryChannel.EMAIL) {
            sendEmailOtp(destination, otp, expiresAt);
            return;
        }

        sendSmsOtp(destination, otp, expiresAt);
    }

    private void sendEmailOtp(String destination, String otp, LocalDateTime expiresAt) {
        JavaMailSender javaMailSender = javaMailSenderProvider.getIfAvailable();
        if (javaMailSender == null) {
            fallbackToLocalLog("EMAIL", destination, otp, expiresAt, null);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(passwordRecoveryProperties.getEmailFrom());
        message.setTo(destination);
        message.setSubject("OTP dat lai mat khau - GoldenHeart Restaurant");
        message.setText(buildEmailBody(otp, expiresAt));

        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            fallbackToLocalLog("EMAIL", destination, otp, expiresAt, exception);
        }
    }

    private void sendSmsOtp(String destination, String otp, LocalDateTime expiresAt) {
        fallbackToLocalLog("SMS", destination, otp, expiresAt, null);
    }

    private void fallbackToLocalLog(String channel, String destination, String otp, LocalDateTime expiresAt, Exception exception) {
        if (!passwordRecoveryProperties.isDevLogDelivery()) {
            throw new IllegalStateException("Kenh gui OTP " + channel + " chua duoc cau hinh de su dung thuc te", exception);
        }

        if (exception == null) {
            log.warn("[DEV-OTP] channel={}, destination={}, otp={}, expiresAt={}",
                    channel, destination, otp, expiresAt.format(OTP_TIME_FORMATTER));
            return;
        }

        log.warn("[DEV-OTP] channel={}, destination={}, otp={}, expiresAt={}, fallbackReason={}",
                channel,
                destination,
                otp,
                expiresAt.format(OTP_TIME_FORMATTER),
                exception.getMessage());
    }

    private String buildEmailBody(String otp, LocalDateTime expiresAt) {
        return """
                Xin chao,

                Ban vua yeu cau dat lai mat khau cho tai khoan GoldenHeart Restaurant.

                Ma OTP cua ban la: %s
                Ma nay het han luc: %s

                Neu ban khong thuc hien yeu cau nay, vui long bo qua email nay.
                """.formatted(otp, expiresAt.format(OTP_TIME_FORMATTER));
    }
}
