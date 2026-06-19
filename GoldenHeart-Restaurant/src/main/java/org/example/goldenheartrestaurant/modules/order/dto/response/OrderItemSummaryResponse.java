package org.example.goldenheartrestaurant.modules.order.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record OrderItemSummaryResponse(
        Integer menuItemId,
        String menuItemName,
        BigDecimal unitPrice,
        Integer quantity,
        String note,
        BigDecimal lineTotal,
        Integer sentQuantity,
        Integer preparingQuantity,
        Integer readyQuantity,
        Integer servedQuantity,
        List<Integer> orderItemIds,
        List<Integer> readyOrderItemIds,
        List<Integer> cancellableOrderItemIds
) {
}
