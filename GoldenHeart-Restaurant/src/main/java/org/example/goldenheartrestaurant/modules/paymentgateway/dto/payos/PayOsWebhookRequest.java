package org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PayOsWebhookRequest(
        String code,
        String desc,
        Boolean success,
        PayOsWebhookData data,
        String signature
) {
}
