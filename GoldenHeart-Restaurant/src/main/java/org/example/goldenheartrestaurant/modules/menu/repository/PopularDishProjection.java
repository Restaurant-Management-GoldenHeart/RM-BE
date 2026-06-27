package org.example.goldenheartrestaurant.modules.menu.repository;

import java.math.BigDecimal;

/**
 * Projection dùng cho native query tổng hợp "món ăn phổ biến" trên homepage.
 * Tên getter phải khớp chính xác (case-insensitive) với alias trong SELECT.
 */
public interface PopularDishProjection {
    Integer getId();
    String getName();
    String getImageUrl();
    String getDescription();
    BigDecimal getPrice();
    Integer getCategoryId();
    String getCategoryName();
    String getProductionStation();
    Long getTotalOrderCount();
}
