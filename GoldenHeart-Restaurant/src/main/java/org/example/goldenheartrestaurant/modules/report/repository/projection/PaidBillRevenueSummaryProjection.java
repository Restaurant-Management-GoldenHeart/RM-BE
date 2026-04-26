package org.example.goldenheartrestaurant.modules.report.repository.projection;

import java.math.BigDecimal;

public interface PaidBillRevenueSummaryProjection {

    Long getPaidBillsCount();

    BigDecimal getPaidBillRevenue();

    BigDecimal getGrossProfit();
}
