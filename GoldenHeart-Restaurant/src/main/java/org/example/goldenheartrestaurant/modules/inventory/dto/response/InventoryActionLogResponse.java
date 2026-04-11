package org.example.goldenheartrestaurant.modules.inventory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryActionLogResponse(
        Integer id,
        Integer inventoryId,
        String actionType,
        String actedByUsername,
        String actedByFullName,
        String branchName,
        String ingredientName,
        String unitSymbol,
        BigDecimal beforeQuantity,
        BigDecimal afterQuantity,
        BigDecimal beforeMinStockLevel,
        BigDecimal afterMinStockLevel,
        BigDecimal beforeReorderLevel,
        BigDecimal afterReorderLevel,
        BigDecimal beforeAverageUnitCost,
        BigDecimal afterAverageUnitCost,
        String beforeIngredientName,
        String afterIngredientName,
        String beforeUnitSymbol,
        String afterUnitSymbol,
        String summary,
        LocalDateTime occurredAt
) {
}
