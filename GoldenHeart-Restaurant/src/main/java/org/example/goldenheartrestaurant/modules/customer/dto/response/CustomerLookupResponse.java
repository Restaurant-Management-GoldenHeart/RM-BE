package org.example.goldenheartrestaurant.modules.customer.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerLookupResponse(
        Integer id,
        String customerCode,
        String name,
        String phone,
        String email,
        Integer loyaltyPoints,
        Integer tierId,
        String tierCode,
        String tierName,
        BigDecimal tierDiscountRate,
        LocalDateTime lastVisitAt
) {
}
