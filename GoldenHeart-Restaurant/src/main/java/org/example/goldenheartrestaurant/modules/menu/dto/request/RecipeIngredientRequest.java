package org.example.goldenheartrestaurant.modules.menu.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RecipeIngredientRequest(
        @NotNull
        Integer ingredientId,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal quantity
) {
}
