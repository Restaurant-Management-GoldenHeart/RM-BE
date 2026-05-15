package org.example.goldenheartrestaurant.modules.billing.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BillHistoryItemResponse(
        Integer billId,
        Integer orderId,
        Integer branchId,
        String branchName,
        Integer tableId,
        String tableName,
        Integer customerId,
        String customerName,
        Integer openedByUserId,
        String openedByName,
        Integer billCreatedByUserId,
        String billCreatedByName,
        String status,
        BigDecimal total,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        BigDecimal grossProfit,
        LocalDateTime lastPaidAt,
        Long paymentCount,
        List<String> paymentMethods
) {
}
