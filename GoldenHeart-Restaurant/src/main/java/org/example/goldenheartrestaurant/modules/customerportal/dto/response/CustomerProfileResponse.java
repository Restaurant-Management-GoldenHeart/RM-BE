package org.example.goldenheartrestaurant.modules.customerportal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Thông tin hồ sơ khách hàng trả về cho customer portal.
 * Bao gồm cả thông tin tier và điểm hiện tại để trang dashboard dùng được mà không cần gọi thêm API.
 */
public record CustomerProfileResponse(
        Integer customerId,
        String name,
        String email,
        String phone,
        String address,
        LocalDate dateOfBirth,
        String gender,
        Integer loyaltyPoints,
        LocalDateTime lastVisitAt,

        // Thông tin tier hiện tại
        String tierCode,
        String tierName,
        BigDecimal tierDiscountRate,

        // Thông tin tier tiếp theo để hiển thị thanh tiến trình
        String nextTierCode,
        String nextTierName,
        Integer nextTierMinPoints,

        LocalDateTime createdAt
) {
}
