package org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos;

public record PayOsItemRequest(
        String name,
        Integer quantity,
        Integer price,
        String unit
) {
}
