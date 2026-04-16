package org.example.goldenheartrestaurant.modules.restaurant.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SplitOrderItemRequest(
        @NotNull
        Integer orderItemId,

        @NotNull
        @Min(1)
        Integer quantity
) {
}
