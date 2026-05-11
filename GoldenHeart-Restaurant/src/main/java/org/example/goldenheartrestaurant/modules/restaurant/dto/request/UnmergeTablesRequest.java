package org.example.goldenheartrestaurant.modules.restaurant.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UnmergeTablesRequest(
        @NotEmpty
        List<@Valid UnmergeTableTargetRequest> targets
) {
}
