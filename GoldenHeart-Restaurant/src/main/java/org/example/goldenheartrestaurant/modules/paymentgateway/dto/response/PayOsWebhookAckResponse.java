package org.example.goldenheartrestaurant.modules.paymentgateway.dto.response;

public record PayOsWebhookAckResponse(
        boolean processed,
        String message
) {
}
