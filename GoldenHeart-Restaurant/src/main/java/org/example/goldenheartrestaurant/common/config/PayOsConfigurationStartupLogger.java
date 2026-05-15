package org.example.goldenheartrestaurant.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Startup runner chỉ để cảnh báo sớm cho tích hợp payOS.
 *
 * Mục tiêu của lớp này không phải chặn boot,
 * mà là log rõ các điểm dễ sai ngay khi server vừa lên:
 * - thiếu credential
 * - webhook URL chưa public
 * - return/cancel URL chưa phù hợp với bối cảnh thanh toán thật
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayOsConfigurationStartupLogger implements ApplicationRunner {

    private final PayOsProperties payOsProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!payOsProperties.isEnabled()) {
            return;
        }

        // Gom toàn bộ log hướng dẫn vào lúc startup để dev nhìn thấy ngay,
        // thay vì chỉ phát hiện lỗi sau khi đã tạo QR hoặc test webhook.
        logCredentialGuidance();
        logWebhookGuidance();
        logResultPageGuidance("return", payOsProperties.getReturnUrl());
        logResultPageGuidance("cancel", payOsProperties.getCancelUrl());
    }

    private void logCredentialGuidance() {
        boolean missingClientId = !StringUtils.hasText(payOsProperties.getClientId());
        boolean missingApiKey = !StringUtils.hasText(payOsProperties.getApiKey());
        boolean missingChecksumKey = !StringUtils.hasText(payOsProperties.getChecksumKey());

        if (missingClientId || missingApiKey || missingChecksumKey) {
            log.error("""
                    [PAYOS] payOS đang được BẬT nhưng credential chưa đầy đủ.
                    [PAYOS] Tạo QR sẽ không gọi được API payOS, và webhook confirm/verify cũng sẽ thất bại.
                    [PAYOS] Cần set đúng cùng một kênh PAYOS_CLIENT_ID, PAYOS_API_KEY, PAYOS_CHECKSUM_KEY mà bạn đang cấu hình webhook trên dashboard.
                    """);
            return;
        }

        log.info("[PAYOS] Credential fingerprint: clientId={}, apiKey={}, checksumKey={}",
                maskSecret(payOsProperties.getClientId()),
                maskSecret(payOsProperties.getApiKey()),
                maskSecret(payOsProperties.getChecksumKey()));
    }

    private void logWebhookGuidance() {
        String webhookUrl = payOsProperties.getWebhookUrl();

        if (!StringUtils.hasText(webhookUrl)) {
            log.warn("""
                    [PAYOS] PAYOS_WEBHOOK_URL đang để trống.
                    [PAYOS] Hệ thống vẫn tạo được QR, nhưng xác nhận realtime sẽ phụ thuộc vào webhook public.
                    [PAYOS] Local khuyến nghị: trỏ biến này về https://<public-be-domain>/api/v1/payment-gateways/payos/webhook
                    [PAYOS] Nếu chưa có webhook public, POS sẽ phải dựa vào bước đồng bộ/reconcile để cập nhật trạng thái.
                    """);
            return;
        }

        if (isClearlyLocalUrl(webhookUrl)) {
            log.warn("""
                    [PAYOS] PAYOS_WEBHOOK_URL đang trỏ vào localhost/127.0.0.1.
                    [PAYOS] Đây không phải địa chỉ mà server PayOS bên ngoài có thể gọi vào.
                    [PAYOS] Hãy đổi sang một URL public HTTPS (ví dụ tunnel local hoặc domain deploy thật).
                    """);
            return;
        }

        if (!isHttpsUrl(webhookUrl)) {
            log.warn("""
                    [PAYOS] PAYOS_WEBHOOK_URL hiện không dùng HTTPS.
                    [PAYOS] Local vẫn có thể test tùy theo hạ tầng của bạn, nhưng production nên dùng domain HTTPS công khai.
                    """);
            return;
        }

        log.info("[PAYOS] Webhook URL đang ở trạng thái phù hợp cho callback public: {}", webhookUrl);
    }

    private void logResultPageGuidance(String type, String url) {
        if (!StringUtils.hasText(url)) {
            log.info("[PAYOS] {} URL fallback đang để trống. FE có thể tự truyền URL theo từng giao dịch.", type);
            return;
        }

        if (isClearlyLocalUrl(url)) {
            log.warn("""
                    [PAYOS] %s URL fallback đang trỏ về localhost.
                    [PAYOS] Cấu hình này chỉ phù hợp khi mở checkout trên cùng máy POS.
                    [PAYOS] Nếu khách quét QR trên thiết bị khác, hãy dùng domain FE public hoặc set VITE_PAYOS_RESULT_BASE_URL.
                    """.formatted(type.toUpperCase(Locale.ROOT)));
            return;
        }

        log.info("[PAYOS] {} URL fallback hiện tại: {}", type, url);
    }

    private boolean isClearlyLocalUrl(String rawUrl) {
        String normalized = rawUrl.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("localhost")
                || normalized.contains("127.0.0.1")
                || normalized.contains("[::1]");
    }

    private boolean isHttpsUrl(String rawUrl) {
        return rawUrl.trim().toLowerCase(Locale.ROOT).startsWith("https://");
    }

    private String maskSecret(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return "<missing>";
        }
        String trimmed = rawValue.trim();
        if (trimmed.length() <= 8) {
            return "****" + trimmed;
        }
        return trimmed.substring(0, 4) + "..." + trimmed.substring(trimmed.length() - 4);
    }
}
