package org.example.goldenheartrestaurant.modules.customerportal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Một đơn hàng trong lịch sử của khách hàng. */
public record CustomerOrderResponse(
        Integer orderId,
        String orderStatus,
        LocalDateTime createdAt,
        LocalDateTime closedAt,

        // Chi nhánh phục vụ
        Integer branchId,
        String branchName,

        // Số bàn — null nếu là đơn mang về
        String tableName,

        // Danh sách món trong đơn
        List<CustomerOrderItemResponse> items,

        // Thông tin thanh toán (lấy từ bill PAID)
        BigDecimal total,
        String billStatus,

        // Điểm tích luỹ từ đơn này (null nếu chưa thanh toán)
        Integer pointsEarned
) {
}
