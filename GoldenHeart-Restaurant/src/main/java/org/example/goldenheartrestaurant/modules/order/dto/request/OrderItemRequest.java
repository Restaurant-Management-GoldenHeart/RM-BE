package org.example.goldenheartrestaurant.modules.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderItemRequest(
        @NotNull
        Integer menuItemId,

        @NotNull
        @Min(1)
        Integer quantity,

        @Size(max = 255)
        String note
) {
}
