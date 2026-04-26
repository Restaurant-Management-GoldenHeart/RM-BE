package org.example.goldenheartrestaurant.modules.customer.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateCustomerTierRequest(
        @NotBlank
        @Size(max = 30)
        String code,

        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        @Min(0)
        Integer minPoints,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal discountRate,

        @NotNull
        Boolean active,

        @Size(max = 255)
        String note
) {
}
