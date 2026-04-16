package org.example.goldenheartrestaurant.modules.order.dto.response;

import java.util.List;

public record OrderItemStatusChangeResponse(
        Integer orderItemId,
        Integer orderId,
        String menuItemName,
        String previousStatus,
        String status,
        List<StockDeductionResponse> deductions
) {
}
