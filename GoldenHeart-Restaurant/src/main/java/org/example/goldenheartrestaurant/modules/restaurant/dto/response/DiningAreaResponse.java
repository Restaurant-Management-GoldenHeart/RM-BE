package org.example.goldenheartrestaurant.modules.restaurant.dto.response;

public record DiningAreaResponse(
        Integer id,
        Integer branchId,
        String branchName,
        String name,
        String code,
        Integer displayOrder,
        Boolean active,
        Integer tableCount
) {
}
