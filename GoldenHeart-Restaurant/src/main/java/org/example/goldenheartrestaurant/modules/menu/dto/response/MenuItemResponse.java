package org.example.goldenheartrestaurant.modules.menu.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MenuItemResponse(
        Integer id,
        Integer branchId,
        String branchName,
        Integer categoryId,
        String categoryName,
        String name,
        String description,
        BigDecimal price,
        String status,
        String effectiveStatus,
        boolean effectiveAvailable,
        List<RecipeResponse> recipes
) {
}
