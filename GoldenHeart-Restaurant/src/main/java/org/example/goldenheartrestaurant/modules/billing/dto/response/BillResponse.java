package org.example.goldenheartrestaurant.modules.billing.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record BillResponse(
        Integer id,
        Integer orderId,
        Integer tableId,
        String tableName,
        Integer customerId,
        String customerName,
        String status,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal manualDiscount,
        BigDecimal loyaltyDiscount,
        BigDecimal total,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        BigDecimal costOfGoodsSold,
        BigDecimal grossProfit,
        Integer appliedTierId,
        String appliedTierCode,
        String appliedTierName,
        BigDecimal appliedTierDiscountRate,
        Boolean loyaltyRewardApplied,
        Integer earnedLoyaltyPoints,
        Integer customerPointsBefore,
        Integer customerPointsAfter,
        List<PaymentResponse> payments
) {
}
