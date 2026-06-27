package org.example.goldenheartrestaurant.modules.customer.dto.response;

import java.math.BigDecimal;
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
        LocalDateTime updatedAt,
        Integer tierId,
        String tierCode,
        String tierName,
        BigDecimal tierDiscountRate,
        /** userId != null nghĩa là khách này đã tự đăng ký tài khoản online. */
        Integer userId,
        Boolean hasAccount
) {
}
