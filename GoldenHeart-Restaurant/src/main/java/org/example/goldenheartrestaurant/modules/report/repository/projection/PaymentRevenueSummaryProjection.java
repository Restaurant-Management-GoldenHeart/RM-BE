package org.example.goldenheartrestaurant.modules.report.repository.projection;

import java.math.BigDecimal;

public interface PaymentRevenueSummaryProjection {

    Long getPaymentCount();

    BigDecimal getCashIn();
}
