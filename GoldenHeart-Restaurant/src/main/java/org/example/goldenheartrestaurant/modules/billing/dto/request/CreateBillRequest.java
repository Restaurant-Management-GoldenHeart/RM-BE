package org.example.goldenheartrestaurant.modules.billing.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateBillRequest(
        @NotNull
        Integer orderId,

        @DecimalMin(value = "0.00")
        BigDecimal discount,

        @DecimalMin(value = "0.00")
        BigDecimal taxRate,

        @DecimalMin(value = "0.00")
        BigDecimal paidAmount,

        @Size(max = 30)
        String paymentMethod
) {
}
