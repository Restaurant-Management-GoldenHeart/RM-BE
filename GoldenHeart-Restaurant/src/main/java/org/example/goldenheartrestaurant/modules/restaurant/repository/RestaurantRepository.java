package org.example.goldenheartrestaurant.modules.restaurant.repository;

import org.example.goldenheartrestaurant.modules.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cơ bản của Restaurant.
 *
 * Ở giai đoạn hiện tại Restaurant đóng vai trò dữ liệu gốc
 * để tổ chức nhiều branch bên dưới.
 */
public interface RestaurantRepository extends JpaRepository<Restaurant, Integer> {
}
