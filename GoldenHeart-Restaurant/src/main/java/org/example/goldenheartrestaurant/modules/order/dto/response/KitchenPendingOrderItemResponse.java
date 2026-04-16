package org.example.goldenheartrestaurant.modules.order.dto.response;

import java.time.LocalDateTime;

public record KitchenPendingOrderItemResponse(
        Integer id,
        Integer orderId,
        Integer tableId,
        String tableName,
        Integer menuItemId,
        String menuItemName,
        Integer quantity,
        String note,
        LocalDateTime createdAt,
        String status
) {
}
