package org.example.goldenheartrestaurant.modules.inventory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InventoryMovementReportResponse(
        Integer branchId,
        String branchName,
        String groupBy,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal totalReceiptValue,
        BigDecimal totalSaleValue,
        BigDecimal totalWasteValue,
        BigDecimal totalAdjustmentInValue,
        BigDecimal totalAdjustmentOutValue,
        BigDecimal totalStocktakeInValue,
        BigDecimal totalStocktakeOutValue,
        BigDecimal totalReturnOutValue,
        BigDecimal totalInValue,
        BigDecimal totalOutValue,
        BigDecimal netMovementValue,
        List<InventoryMovementPeriodResponse> periods,
        LocalDateTime generatedAt
) {
}
