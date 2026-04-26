package org.example.goldenheartrestaurant.modules.report.repository.projection;

import org.example.goldenheartrestaurant.modules.billing.entity.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentMethodBreakdownProjection {

    PaymentMethod getMethod();

    Long getPaymentCount();

    BigDecimal getTotalAmount();
}
