package org.example.goldenheartrestaurant.modules.restaurant.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateRestaurantTableRequest(
        @NotNull
        Integer branchId,

        Integer areaId,

        @NotBlank
        @Size(max = 20)
        String tableNumber,

        @Min(1)
        Integer capacity,

        @DecimalMin(value = "0.00")
        BigDecimal posX,

        @DecimalMin(value = "0.00")
        BigDecimal posY,

        @DecimalMin(value = "0.00")
        BigDecimal width,

        @DecimalMin(value = "0.00")
        BigDecimal height,

        @Min(0)
        Integer displayOrder
) {
}
