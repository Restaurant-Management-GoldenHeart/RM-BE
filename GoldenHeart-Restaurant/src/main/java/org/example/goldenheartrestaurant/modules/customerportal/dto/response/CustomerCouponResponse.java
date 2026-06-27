package org.example.goldenheartrestaurant.modules.customerportal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Một coupon trong ví khách hàng. */
public record CustomerCouponResponse(
        Integer customerCouponId,
        String couponCode,
        String couponName,
        String description,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        LocalDateTime endDate,
        String status,
        LocalDateTime receivedAt,
        LocalDateTime usedAt,

        /** True nếu coupon còn hạn và chưa dùng. */
        boolean usable
) {
}
