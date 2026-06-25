package org.example.goldenheartrestaurant.modules.menu.dto.response;

public record CategoryResponse(
        Integer id,
        String name,
        String description,
        String productionStation,
        int menuItemCount
) {
}