package org.example.goldenheartrestaurant.modules.paymentgateway.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentGatewayTransactionResponse(
        Integer transactionId,
        Integer billId,
        Integer orderId,
        Integer tableId,
        String tableName,
        String provider,
        String paymentMethod,
        Long providerOrderCode,
        String providerPaymentLinkId,
        BigDecimal requestedAmount,
        BigDecimal paidAmount,
        String status,
        String checkoutUrl,
        String qrCode,
        String deepLink,
        LocalDateTime expiredAt,
        LocalDateTime confirmedAt,
        LocalDateTime cancelledAt,
        String failureReason,
        String providerReference,
        String providerCode,
        String providerMessage,
        Integer paymentId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
