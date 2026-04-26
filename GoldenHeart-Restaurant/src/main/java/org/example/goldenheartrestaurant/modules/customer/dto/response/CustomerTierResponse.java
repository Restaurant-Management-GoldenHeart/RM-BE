package org.example.goldenheartrestaurant.modules.customer.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerTierResponse(
        Integer id,
        String code,
        String name,
        Integer minPoints,
        BigDecimal discountRate,
        Boolean active,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
