package org.example.goldenheartrestaurant.modules.restaurant.dto.response;

import java.math.BigDecimal;

public record UnmergeTableResultResponse(
        Integer tableId,
        String tableName,
        String tableDisplayName,
        String tableStatus,
        Integer orderId,
        String orderStatus,
        BigDecimal subtotal
) {
}
