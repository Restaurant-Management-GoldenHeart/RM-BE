package org.example.goldenheartrestaurant.modules.inventory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InventoryImportPreviewResponse(
        Integer branchId,
        String branchName,
        LocalDate receiptDate,
        String invoiceNumber,
        String note,
        int totalRows,
        int validRows,
        int invalidRows,
        BigDecimal totalQuantity,
        BigDecimal totalAmount,
        boolean importable,
        List<String> globalErrors,
        List<InventoryImportRowResponse> rows
) {
}