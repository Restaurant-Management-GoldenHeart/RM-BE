package org.example.goldenheartrestaurant.modules.menu.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateMenuItemRequest(
        Integer branchId,

        Integer categoryId,

        @Size(max = 150)
        String name,

        @Size(max = 500)
        String description,

        @DecimalMin(value = "0.00")
        BigDecimal price,

        String status,

        @Valid
        List<RecipeIngredientRequest> recipes
) {
}
