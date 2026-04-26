package org.example.goldenheartrestaurant.modules.report.repository.projection;

import org.example.goldenheartrestaurant.modules.billing.entity.BillStatus;

public interface BillStatusCountProjection {

    BillStatus getStatus();

    Long getTotal();
}
