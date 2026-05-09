package org.example.goldenheartrestaurant.common.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.payos")
public class PayOsProperties {

    private boolean enabled = false;

    private String clientId;

    private String apiKey;

    private String checksumKey;

    private String baseUrl = "https://api-merchant.payos.vn";

    private String returnUrl = "http://localhost:3000/payment-success";

    private String cancelUrl = "http://localhost:3000/payment-cancel";

    private String webhookUrl = "http://localhost:1010/api/v1/payment-gateways/payos/webhook";

    private String partnerCode;

    private Duration requestTimeout = Duration.ofSeconds(10);

    @Min(1)
    private int defaultExpireMinutes = 15;
}
