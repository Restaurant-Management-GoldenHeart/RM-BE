package org.example.goldenheartrestaurant.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

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
                    [PAYOS] payOS dang duoc BAT nhung credential chua day du.
                    [PAYOS] Tao QR se khong goi duoc API payOS, va webhook confirm/verify cung se that bai.
                    [PAYOS] Can set dung cung mot kenh PAYOS_CLIENT_ID, PAYOS_API_KEY, PAYOS_CHECKSUM_KEY ma ban dang cau hinh webhook tren dashboard.
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
                    [PAYOS] PAYOS_WEBHOOK_URL dang de trong.
                    [PAYOS] He thong van tao duoc QR, nhung xac nhan realtime se phu thuoc vao webhook public.
                    [PAYOS] Local khuyen nghi: tro bien nay ve https://<public-be-domain>/api/v1/payment-gateways/payos/webhook
                    [PAYOS] Neu chua co webhook public, POS se phai dua vao buoc dong bo/reconcile de cap nhat trang thai.
                    """);
            return;
        }

        if (isClearlyLocalUrl(webhookUrl)) {
            log.warn("""
                    [PAYOS] PAYOS_WEBHOOK_URL dang tro vao localhost/127.0.0.1.
                    [PAYOS] Day khong phai dia chi ma server PayOS ben ngoai co the goi vao.
                    [PAYOS] Hay doi sang mot URL public HTTPS (vi du tunnel local hoac domain deploy that).
                    """);
            return;
        }

        if (!isHttpsUrl(webhookUrl)) {
            log.warn("""
                    [PAYOS] PAYOS_WEBHOOK_URL hien khong dung HTTPS.
                    [PAYOS] Local van co the test tuy theo ha tang cua ban, nhung production nen dung domain HTTPS cong khai.
                    """);
            return;
        }

        log.info("[PAYOS] Webhook URL dang o trang thai phu hop cho callback public: {}", webhookUrl);
    }

    private void logResultPageGuidance(String type, String url) {
        if (!StringUtils.hasText(url)) {
            log.info("[PAYOS] {} URL fallback dang de trong. FE co the tu truyen URL theo tung giao dich.", type);
            return;
        }

        if (isClearlyLocalUrl(url)) {
            log.warn("""
                    [PAYOS] %s URL fallback dang tro ve localhost.
                    [PAYOS] Cau hinh nay chi phu hop khi mo checkout tren cung may POS.
                    [PAYOS] Neu khach quet QR tren thiet bi khac, hay dung domain FE public hoac set VITE_PAYOS_RESULT_BASE_URL.
                    """.formatted(type.toUpperCase(Locale.ROOT)));
            return;
        }

        log.info("[PAYOS] {} URL fallback hien tai: {}", type, url);
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
