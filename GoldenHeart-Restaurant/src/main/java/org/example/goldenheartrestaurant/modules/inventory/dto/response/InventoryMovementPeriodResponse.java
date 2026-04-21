package org.example.goldenheartrestaurant.modules.inventory.dto.response;

import java.math.BigDecimal;

public record InventoryMovementPeriodResponse(
        String periodKey,
        BigDecimal receiptValue,
        BigDecimal saleValue,
        BigDecimal wasteValue,
        BigDecimal adjustmentInValue,
        BigDecimal adjustmentOutValue,
        BigDecimal stocktakeInValue,
        BigDecimal stocktakeOutValue,
        BigDecimal returnOutValue,
        BigDecimal totalInValue,
        BigDecimal totalOutValue,
        BigDecimal netMovementValue
) {
}
