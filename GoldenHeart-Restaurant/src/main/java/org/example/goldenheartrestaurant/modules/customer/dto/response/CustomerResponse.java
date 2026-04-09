package org.example.goldenheartrestaurant.modules.customer.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerResponse(
        Integer id,
        String customerCode,
        String name,
        String phone,
        String email,
        Integer loyaltyPoints,
        String address,
        LocalDate dateOfBirth,
        String gender,
        String note,
        LocalDateTime lastVisitAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
