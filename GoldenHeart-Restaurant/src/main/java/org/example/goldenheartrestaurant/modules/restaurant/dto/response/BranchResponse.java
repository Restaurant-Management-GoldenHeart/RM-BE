package org.example.goldenheartrestaurant.modules.restaurant.dto.response;

public record BranchResponse(
        Integer id,
        Integer restaurantId,
        String restaurantName,
        String name,
        String address,
        String phone
) {
}
