package org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PayOsPaymentLinkData(
        @JsonAlias({"id", "paymentLinkId"})
        String id,
        Long orderCode,
        Integer amount,
        Integer amountPaid,
        Integer amountRemaining,
        String status,
        String checkoutUrl,
        String qrCode,
        String deepLink,
        String description,
        Long expiredAt
) {
}
