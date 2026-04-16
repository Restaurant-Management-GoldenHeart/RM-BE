package org.example.goldenheartrestaurant.modules.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        Integer branchId,
        Integer tableId,
        Integer customerId,

        @Valid
        @NotEmpty
        List<OrderItemRequest> items
) {
}
