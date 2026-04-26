package org.example.goldenheartrestaurant.modules.report.dto.response;

import java.math.BigDecimal;

public record PaymentMethodBreakdownItemResponse(
        String method,
        Long paymentCount,
        BigDecimal totalAmount,
        BigDecimal percentage
) {
}
