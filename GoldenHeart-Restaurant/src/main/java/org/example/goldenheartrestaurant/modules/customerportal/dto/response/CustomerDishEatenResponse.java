package org.example.goldenheartrestaurant.modules.customerportal.dto.response;

import java.time.LocalDateTime;

/** Một món ăn khách đã từng gọi, kèm thống kê tổng số lần và lần gần nhất. */
public record CustomerDishEatenResponse(
        Integer menuItemId,
        String name,
        String imageUrl,
        String categoryName,
        Long totalQuantity,
        LocalDateTime lastOrderedAt
) {
}
