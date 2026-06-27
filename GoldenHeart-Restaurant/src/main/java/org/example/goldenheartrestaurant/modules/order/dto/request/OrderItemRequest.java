package org.example.goldenheartrestaurant.modules.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotNull
        Integer menuItemId,

        @NotNull
        @Min(1)
        Integer quantity,

        @Size(max = 255)
        String note,

        /**
         * Giá ghi đè — dùng cho combo items (giá đã áp dụng chiết khấu).
         * Nếu null → dùng giá gốc của MenuItem trong DB.
         * FE chỉ gửi field này khi item thuộc về combo để đảm bảo tổng tiền đúng.
         */
        BigDecimal overridePrice
) {
}
