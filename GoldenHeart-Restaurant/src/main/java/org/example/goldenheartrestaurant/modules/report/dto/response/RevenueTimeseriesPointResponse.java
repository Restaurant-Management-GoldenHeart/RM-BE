package org.example.goldenheartrestaurant.modules.report.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueTimeseriesPointResponse(
        String periodKey,
        LocalDate fromDate,
        LocalDate toDate,
        Long paymentCount,
        BigDecimal cashIn,
        Long paidBillsCount,
        BigDecimal paidBillRevenue,
        BigDecimal grossProfit,
        BigDecimal averagePaidBillValue
) {
}
