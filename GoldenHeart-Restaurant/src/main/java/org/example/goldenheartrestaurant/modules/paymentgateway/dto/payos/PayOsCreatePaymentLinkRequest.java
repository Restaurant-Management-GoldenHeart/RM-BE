package org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos;

import java.util.List;

public record PayOsCreatePaymentLinkRequest(
        Long orderCode,
        Integer amount,
        String description,
        String buyerName,
        String buyerEmail,
        String buyerPhone,
        List<PayOsItemRequest> items,
        String cancelUrl,
        String returnUrl,
        Long expiredAt,
        String signature
) {
}
