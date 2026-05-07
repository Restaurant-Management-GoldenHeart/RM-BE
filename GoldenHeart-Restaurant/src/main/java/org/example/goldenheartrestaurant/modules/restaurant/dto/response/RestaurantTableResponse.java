package org.example.goldenheartrestaurant.modules.restaurant.dto.response;

import java.math.BigDecimal;
import java.util.List;

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
        String status,
        Boolean merged,
        Boolean mergeRoot,
        Integer mergeRootTableId,
        String mergeRootTableName,
        String displayName,
        List<Integer> mergedTableIds,
        List<String> mergedTableNames
) {
}
