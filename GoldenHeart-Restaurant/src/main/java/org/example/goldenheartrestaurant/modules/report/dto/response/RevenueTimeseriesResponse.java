package org.example.goldenheartrestaurant.modules.report.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RevenueTimeseriesResponse(
        Integer branchId,
        String branchName,
        String groupBy,
        LocalDate fromDate,
        LocalDate toDate,
        Long totalPaymentCount,
        BigDecimal totalCashIn,
        Long totalPaidBillsCount,
        BigDecimal totalPaidBillRevenue,
        BigDecimal totalGrossProfit,
        List<RevenueTimeseriesPointResponse> points,
        LocalDateTime generatedAt
) {
}
