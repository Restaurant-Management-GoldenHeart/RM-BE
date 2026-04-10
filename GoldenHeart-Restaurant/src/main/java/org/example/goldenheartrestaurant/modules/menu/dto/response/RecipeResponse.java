package org.example.goldenheartrestaurant.modules.menu.dto.response;

import java.math.BigDecimal;

public record RecipeResponse(
        Integer ingredientId,
        String ingredientName,
        String unit,
        BigDecimal quantity
) {
}
