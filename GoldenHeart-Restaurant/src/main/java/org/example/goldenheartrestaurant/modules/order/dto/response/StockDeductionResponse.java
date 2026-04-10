package org.example.goldenheartrestaurant.modules.order.dto.response;

import java.math.BigDecimal;

public record StockDeductionResponse(
        Integer ingredientId,
        String ingredientName,
        String unit,
        BigDecimal deductedQuantity,
        BigDecimal remainingQuantity
) {
}
