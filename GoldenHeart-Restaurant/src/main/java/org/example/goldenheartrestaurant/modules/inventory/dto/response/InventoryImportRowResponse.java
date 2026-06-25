package org.example.goldenheartrestaurant.modules.inventory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InventoryImportRowResponse(
        int rowNumber,
        boolean valid,
        String ingredientName,
        String unit,
        Integer unitId,
        String unitName,
        String unitSymbol,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal lineTotal,
        BigDecimal purchaseQuantity,
        String purchaseUnit,
        Integer purchaseUnitId,
        String purchaseUnitName,
        String purchaseUnitSymbol,
        BigDecimal purchaseToBaseRate,
        BigDecimal convertedQuantity,
        BigDecimal purchaseUnitCost,
        BigDecimal minStockLevel,
        BigDecimal reorderLevel,
        LocalDate expiryDate,
        String batchNumber,
        String note,
        String action,
        Integer ingredientId,
        Integer inventoryId,
        BigDecimal currentQuantity,
        BigDecimal quantityAfterImport,
        BigDecimal averageUnitCostAfterImport,
        List<String> errors,
        List<String> warnings
) {
}
