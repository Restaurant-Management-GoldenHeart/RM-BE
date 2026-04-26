package org.example.goldenheartrestaurant.modules.restaurant.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDiningAreaRequest(
        @NotNull
        Integer branchId,

        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Size(max = 30)
        String code,

        @Min(0)
        Integer displayOrder,

        Boolean active
) {
}
