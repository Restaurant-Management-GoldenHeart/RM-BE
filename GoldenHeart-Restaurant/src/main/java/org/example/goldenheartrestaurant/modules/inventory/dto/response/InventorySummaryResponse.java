package org.example.goldenheartrestaurant.modules.inventory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventorySummaryResponse(
        Integer branchId,
        String branchName,
        Long totalItems,
        BigDecimal totalQuantity,
        BigDecimal totalInventoryValue,
        Long lowStockCount,
        Long outOfStockCount,
        LocalDateTime generatedAt
) {
}
