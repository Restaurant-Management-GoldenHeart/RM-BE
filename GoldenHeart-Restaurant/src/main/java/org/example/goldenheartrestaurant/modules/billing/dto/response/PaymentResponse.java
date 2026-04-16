package org.example.goldenheartrestaurant.modules.billing.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Integer id,
        BigDecimal amount,
        String method,
        LocalDateTime paidAt
) {
}
