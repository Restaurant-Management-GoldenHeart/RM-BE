package org.example.goldenheartrestaurant.modules.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateInventoryItemRequest(
        @NotNull
        Integer branchId,

        @NotBlank
        @Size(max = 100)
        String ingredientName,

        @NotNull
        Integer unitId,

        @NotNull
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
