package org.example.goldenheartrestaurant.modules.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateInventoryItemRequest(
        @Size(max = 100)
        String ingredientName,

        Integer unitId,

        @DecimalMin(value = "0.00")
        BigDecimal quantity,

        @DecimalMin(value = "0.00")
        BigDecimal minStockLevel,

        @DecimalMin(value = "0.00")
        BigDecimal reorderLevel,

        @DecimalMin(value = "0.00")
        BigDecimal averageUnitCost,

        @Size(max = 500)
        String note
) {
}
