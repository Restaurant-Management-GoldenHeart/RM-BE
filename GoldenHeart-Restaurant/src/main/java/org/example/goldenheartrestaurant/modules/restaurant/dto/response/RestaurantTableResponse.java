package org.example.goldenheartrestaurant.modules.restaurant.dto.response;

import java.math.BigDecimal;

public record RestaurantTableResponse(
        Integer id,
        Integer branchId,
        String branchName,
        Integer areaId,
        String areaName,
        String tableNumber,
        Integer capacity,
        BigDecimal posX,
        BigDecimal posY,
        BigDecimal width,
        BigDecimal height,
        Integer displayOrder,
        String status
) {
}
