package org.example.goldenheartrestaurant.common.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Nhóm cấu hình dành riêng cho tích hợp payOS.
 *
 * Tách riêng thành {@code ConfigurationProperties} giúp:
 * - service thanh toán không phải hard-code URL, timeout hay credential
 * - mỗi môi trường có thể override qua env hoặc file cấu hình
 * - các lớp như startup logger, client và payment gateway cùng đọc một nguồn thống nhất
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.payos")
public class PayOsProperties {

    private boolean enabled = false;

    private String clientId = "a703d984-ca43-4ae5-a473-c9c2fcdc13fa";

    private String apiKey = "930b93d0-865d-407c-b732-951c920c13cb";

    private String checksumKey = "a593dc88b912f7d89681061d22ca8b15c33e33d352f3057725911c27aafabd72";

    private String baseUrl = "https://api-merchant.payos.vn";

    private String returnUrl = "https://rm-fe.vercel.app/payment-success";

    private String cancelUrl = "https://rm-fe.vercel.app/payment-cancel";

    private String webhookUrl = "";

    private String partnerCode;

    private Duration requestTimeout = Duration.ofSeconds(10);

    @Min(1)
    private int defaultExpireMinutes = 15;
}
