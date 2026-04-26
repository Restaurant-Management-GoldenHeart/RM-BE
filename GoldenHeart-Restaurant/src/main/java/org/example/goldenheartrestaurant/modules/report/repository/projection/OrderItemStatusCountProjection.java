package org.example.goldenheartrestaurant.modules.report.repository.projection;

import org.example.goldenheartrestaurant.modules.order.entity.OrderItemStatus;

public interface OrderItemStatusCountProjection {

    OrderItemStatus getStatus();

    Long getTotal();
}
