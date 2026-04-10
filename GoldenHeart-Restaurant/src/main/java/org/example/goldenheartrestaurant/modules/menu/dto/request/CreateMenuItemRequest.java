package org.example.goldenheartrestaurant.modules.menu.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateMenuItemRequest(
        @NotNull
        Integer branchId,

        @NotNull
        Integer categoryId,

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 500)
        String description,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal price,

        String status,

        @Valid
        @NotEmpty
        List<RecipeIngredientRequest> recipes
) {
}
