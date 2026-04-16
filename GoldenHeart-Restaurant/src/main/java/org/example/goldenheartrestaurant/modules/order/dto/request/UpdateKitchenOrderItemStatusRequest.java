package org.example.goldenheartrestaurant.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateKitchenOrderItemStatusRequest(
        @NotBlank
        String status,

        @Size(max = 255)
        String reason
) {
}
