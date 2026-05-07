package org.example.goldenheartrestaurant.modules.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Integer orderId,
        Integer branchId,
        String branchName,
        Integer tableId,
        String tableName,
        String tableDisplayName,
        List<Integer> mergedTableIds,
        List<String> mergedTableNames,
        Integer customerId,
        String customerName,
        String status,
        LocalDateTime createdAt,
        LocalDateTime closedAt,
        BigDecimal subtotal,
        List<OrderItemResponse> items
) {
}
