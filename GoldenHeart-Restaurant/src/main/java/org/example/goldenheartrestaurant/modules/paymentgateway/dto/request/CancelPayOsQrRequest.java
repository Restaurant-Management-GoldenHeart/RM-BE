package org.example.goldenheartrestaurant.modules.paymentgateway.dto.request;

import jakarta.validation.constraints.Size;

public record CancelPayOsQrRequest(
        @Size(max = 255)
        String reason
) {
}
