package org.example.goldenheartrestaurant.modules.restaurant.dto.response;

import java.util.List;

public record UnmergeTablesResponse(
        String action,
        Integer rootTableId,
        String rootTableName,
        String rootTableDisplayName,
        Integer sourceOrderId,
        String sourceOrderStatus,
        List<UnmergeTableResultResponse> tables
) {
}
