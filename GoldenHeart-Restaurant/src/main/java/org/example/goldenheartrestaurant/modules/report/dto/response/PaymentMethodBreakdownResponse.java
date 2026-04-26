package org.example.goldenheartrestaurant.modules.report.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentMethodBreakdownResponse(
        Integer branchId,
        String branchName,
        String periodType,
        LocalDate fromDate,
        LocalDate toDate,
        Long totalPaymentCount,
        BigDecimal totalAmount,
        List<PaymentMethodBreakdownItemResponse> items,
        LocalDateTime generatedAt
) {
}
