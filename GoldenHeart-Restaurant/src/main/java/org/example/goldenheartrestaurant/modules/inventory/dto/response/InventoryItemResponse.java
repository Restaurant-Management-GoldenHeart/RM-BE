package org.example.goldenheartrestaurant.modules.inventory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryItemResponse(
        Integer inventoryId,
        Integer branchId,
        String branchName,
        Integer ingredientId,
        String ingredientName,
        Integer unitId,
        String unitName,
        String unitSymbol,
        BigDecimal quantity,
        BigDecimal minStockLevel,
        BigDecimal reorderLevel,
        BigDecimal averageUnitCost,
        boolean lowStock,
        boolean outOfStock,
        String alertMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
