package org.example.goldenheartrestaurant.modules.menu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 2000)
        String description,

        String productionStation
) {
}