package org.example.goldenheartrestaurant.modules.customer.dto.response;

import java.time.LocalDateTime;

public record CustomerLoyaltyTransactionResponse(
        Integer id,
        Integer customerId,
        Integer billId,
        String type,
        Integer pointsDelta,
        Integer pointsBefore,
        Integer pointsAfter,
        String description,
        LocalDateTime createdAt
) {
}
