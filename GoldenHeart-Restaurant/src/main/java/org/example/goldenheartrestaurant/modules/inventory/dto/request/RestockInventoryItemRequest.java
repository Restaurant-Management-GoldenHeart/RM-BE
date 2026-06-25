package org.example.goldenheartrestaurant.modules.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RestockInventoryItemRequest(
        @NotNull
        @DecimalMin(value = "0.000001")
        BigDecimal purchaseQuantity,

        @NotNull
        Integer purchaseUnitId,

        @DecimalMin(value = "0.000001")
        BigDecimal purchaseToBaseRate,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal purchaseUnitCost,

        LocalDate receiptDate,

        @Size(max = 50)
        String invoiceNumber,

        @Size(max = 50)
        String batchNumber,

        LocalDate expiryDate,

        Boolean saveAsDefaultPurchaseUnit,

        @Size(max = 500)
        String note
) {
}
