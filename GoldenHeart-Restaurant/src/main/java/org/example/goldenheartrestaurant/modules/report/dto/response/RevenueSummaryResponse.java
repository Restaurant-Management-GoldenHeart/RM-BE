package org.example.goldenheartrestaurant.modules.report.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RevenueSummaryResponse(
        Integer branchId,
        String branchName,
        String periodType,
        LocalDate fromDate,
        LocalDate toDate,
        Long paymentCount,
        BigDecimal cashIn,
        Long paidBillsCount,
        BigDecimal paidBillRevenue,
        BigDecimal grossProfit,
        BigDecimal averagePaidBillValue,
        LocalDateTime generatedAt
) {
}
