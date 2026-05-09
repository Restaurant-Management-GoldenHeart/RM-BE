package org.example.goldenheartrestaurant.modules.paymentgateway.dto.request;

import jakarta.validation.constraints.Size;

public record CreatePayOsQrRequest(
        @Size(max = 1000)
        String returnUrl,

        @Size(max = 1000)
        String cancelUrl
) {
}
