package org.example.goldenheartrestaurant.modules.restaurant.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SplitTableRequest(
        @NotNull
        Integer targetTableId,

        @NotEmpty
        List<@Valid SplitOrderItemRequest> items
) {
}
