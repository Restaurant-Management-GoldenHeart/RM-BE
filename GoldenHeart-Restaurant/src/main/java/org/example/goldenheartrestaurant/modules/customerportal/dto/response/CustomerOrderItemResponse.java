package org.example.goldenheartrestaurant.modules.customerportal.dto.response;

import java.math.BigDecimal;

/** Một dòng món ăn trong đơn hàng lịch sử — đủ thông tin để khách nhận ra món đã gọi. */
public record CustomerOrderItemResponse(
        Integer orderItemId,
        Integer menuItemId,
        String menuItemName,
        String menuItemImageUrl,
        String categoryName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String status,
        String note,

        /** Khách đã đánh giá dòng này chưa — dùng để hiển thị nút "Viết đánh giá" hay không. */
        boolean reviewed
) {
}
