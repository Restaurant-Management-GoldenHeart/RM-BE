package org.example.goldenheartrestaurant.modules.order.dto.response;

import java.util.List;

public record OrderItemCompletionResponse(
        Integer orderItemId,
        Integer orderId,
        String menuItemName,
        String status,
        List<StockDeductionResponse> deductions
) {
}
