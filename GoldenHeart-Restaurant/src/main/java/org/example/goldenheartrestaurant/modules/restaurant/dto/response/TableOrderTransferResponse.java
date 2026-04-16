package org.example.goldenheartrestaurant.modules.restaurant.dto.response;

import java.math.BigDecimal;

public record TableOrderTransferResponse(
        String action,
        Integer sourceTableId,
        String sourceTableName,
        String sourceTableStatus,
        Integer sourceOrderId,
        String sourceOrderStatus,
        BigDecimal sourceSubtotal,
        Integer targetTableId,
        String targetTableName,
        String targetTableStatus,
        Integer targetOrderId,
        String targetOrderStatus,
        BigDecimal targetSubtotal
) {
}
