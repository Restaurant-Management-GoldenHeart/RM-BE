package org.example.goldenheartrestaurant.modules.combo.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ComboItemRequest(
        @NotNull Integer menuItemId,
        @Min(1) int quantity
) {
}
