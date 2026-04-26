package org.example.goldenheartrestaurant.modules.billing.dto.response;

import java.math.BigDecimal;

public record CheckoutPreviewResponse(
        Integer orderId,
        Integer customerId,
        String customerName,
        Integer currentTierId,
        String currentTierCode,
        String currentTierName,
        BigDecimal currentTierDiscountRate,
        Integer projectedTierId,
        String projectedTierCode,
        String projectedTierName,
        BigDecimal projectedTierDiscountRate,
        Integer currentPoints,
        Integer earnedLoyaltyPoints,
        Integer projectedPointsAfterPayment,
        BigDecimal subtotal,
        BigDecimal taxRate,
        BigDecimal tax,
        BigDecimal manualDiscount,
        BigDecimal loyaltyDiscount,
        BigDecimal totalDiscount,
        BigDecimal total,
        Boolean applyLoyaltyDiscount
) {
}
