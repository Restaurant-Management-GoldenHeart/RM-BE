package org.example.goldenheartrestaurant.modules.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTableStatusRequest(
        @NotBlank
        @Size(max = 20)
        String status
) {
}
