package org.example.goldenheartrestaurant.modules.inventory.repository.projection;

import java.math.BigDecimal;

public interface InventorySummaryProjection {

    Long getTotalItems();

    BigDecimal getTotalQuantity();

    BigDecimal getTotalInventoryValue();

    Long getLowStockCount();

    Long getOutOfStockCount();
}
