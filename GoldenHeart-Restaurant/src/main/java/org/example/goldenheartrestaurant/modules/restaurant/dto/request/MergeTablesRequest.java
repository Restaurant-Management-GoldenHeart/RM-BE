package org.example.goldenheartrestaurant.modules.restaurant.dto.request;

import jakarta.validation.constraints.NotNull;

public record MergeTablesRequest(
        @NotNull
        Integer sourceTableId,

        @NotNull
        Integer targetTableId
) {
}
