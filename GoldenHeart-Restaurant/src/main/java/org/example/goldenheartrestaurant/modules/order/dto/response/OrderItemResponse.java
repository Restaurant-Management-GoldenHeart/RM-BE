package org.example.goldenheartrestaurant.modules.order.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        Integer id,
        Integer menuItemId,
        String menuItemName,
        BigDecimal unitPrice,
        Integer quantity,
        String note,
        String status,
        BigDecimal lineTotal
) {
}
