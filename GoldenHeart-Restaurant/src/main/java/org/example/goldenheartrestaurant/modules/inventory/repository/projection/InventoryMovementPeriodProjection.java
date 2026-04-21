package org.example.goldenheartrestaurant.modules.inventory.repository.projection;

import java.math.BigDecimal;

public interface InventoryMovementPeriodProjection {

    String getPeriodKey();

    BigDecimal getReceiptValue();

    BigDecimal getSaleValue();

    BigDecimal getWasteValue();

    BigDecimal getAdjustmentInValue();

    BigDecimal getAdjustmentOutValue();

    BigDecimal getStocktakeInValue();

    BigDecimal getStocktakeOutValue();

    BigDecimal getReturnOutValue();
}
