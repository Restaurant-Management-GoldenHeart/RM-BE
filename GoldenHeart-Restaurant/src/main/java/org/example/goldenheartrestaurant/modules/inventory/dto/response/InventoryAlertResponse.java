package org.example.goldenheartrestaurant.modules.inventory.dto.response;

import java.math.BigDecimal;

public record InventoryAlertResponse(
        Integer inventoryId,
        Integer branchId,
        String branchName,
        Integer ingredientId,
        String ingredientName,
        String unitSymbol,
        BigDecimal currentQuantity,
        BigDecimal minStockLevel,
        BigDecimal reorderLevel,
        InventoryAlertLevel level,
        String message
) {
}
