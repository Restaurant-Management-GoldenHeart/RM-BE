package org.example.goldenheartrestaurant.modules.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBranchRequest(
        @NotNull
        Integer restaurantId,

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 255)
        String address,

        @Size(max = 20)
        @Pattern(regexp = "^[0-9+\\-() ]{0,20}$", message = "Phone number is invalid")
        String phone
) {
}
