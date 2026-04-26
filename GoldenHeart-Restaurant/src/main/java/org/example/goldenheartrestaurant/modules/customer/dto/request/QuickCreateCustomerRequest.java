package org.example.goldenheartrestaurant.modules.customer.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record QuickCreateCustomerRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @Email
        @Size(max = 100)
        String email,

        @Pattern(regexp = "^[0-9+\\-() ]{8,20}$", message = "Phone number is invalid")
        String phone,

        @Size(max = 255)
        String note
) {
}
