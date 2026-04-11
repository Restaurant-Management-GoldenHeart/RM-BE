package org.example.goldenheartrestaurant.modules.inventory.dto.response;

public record MeasurementUnitResponse(
        Integer id,
        String code,
        String name,
        String symbol,
        String description
) {
}
