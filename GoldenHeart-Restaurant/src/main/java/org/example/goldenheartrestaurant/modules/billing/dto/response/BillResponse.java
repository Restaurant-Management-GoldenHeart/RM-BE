package org.example.goldenheartrestaurant.modules.billing.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record BillResponse(
        Integer id,
        Integer orderId,
        Integer tableId,
        String tableName,
        String status,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal total,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        BigDecimal costOfGoodsSold,
        BigDecimal grossProfit,
        List<PaymentResponse> payments
) {
}
