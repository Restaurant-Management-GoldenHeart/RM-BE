package org.example.goldenheartrestaurant.modules.report.repository.projection;

import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTableStatus;

public interface TableStatusCountProjection {

    RestaurantTableStatus getStatus();

    Long getTotal();
}
