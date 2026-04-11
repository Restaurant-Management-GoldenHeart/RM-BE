package org.example.goldenheartrestaurant.modules.order.repository;

import org.example.goldenheartrestaurant.modules.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cơ bản của Order.
 *
 * Hiện tại luồng order phức tạp chủ yếu đi qua OrderItemRepository,
 * còn OrderRepository vẫn đủ dùng với CRUD mặc định.
 */
public interface OrderRepository extends JpaRepository<Order, Integer> {
}
