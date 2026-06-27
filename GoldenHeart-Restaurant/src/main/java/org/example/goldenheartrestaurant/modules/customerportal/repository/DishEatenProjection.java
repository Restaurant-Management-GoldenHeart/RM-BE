package org.example.goldenheartrestaurant.modules.customerportal.repository;

import java.time.LocalDateTime;

/**
 * Projection dùng cho query thống kê "Món tôi đã ăn".
 *
 * Hibernate sẽ map kết quả GROUP BY trực tiếp vào các getter này
 * mà không cần tải toàn bộ entity — giữ query nhẹ và nhanh.
 */
public interface DishEatenProjection {

    Integer getMenuItemId();

    String getName();

    String getImageUrl();

    String getCategoryName();

    /** Tổng số lượng gọi qua tất cả đơn hàng. */
    Long getTotalQuantity();

    /** Lần gọi món gần nhất. */
    LocalDateTime getLastOrderedAt();
}
