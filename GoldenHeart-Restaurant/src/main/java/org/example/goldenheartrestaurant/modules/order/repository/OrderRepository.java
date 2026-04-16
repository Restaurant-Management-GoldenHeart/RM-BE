package org.example.goldenheartrestaurant.modules.order.repository;

import org.example.goldenheartrestaurant.modules.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository cơ bản của Order.
 *
 * Hiện tại luồng order phức tạp chủ yếu đi qua OrderItemRepository,
 * còn OrderRepository vẫn đủ dùng với CRUD mặc định.
 */
public interface OrderRepository extends JpaRepository<Order, Integer> {

    @Query("""
            select distinct o
            from Order o
            join fetch o.branch b
            left join fetch o.table t
            left join fetch o.customer c
            left join fetch o.orderItems oi
            left join fetch oi.menuItem mi
            where o.id = :orderId
            """)
    Optional<Order> findDetailById(@Param("orderId") Integer orderId);

    @Query("""
            select distinct o
            from Order o
            join fetch o.branch b
            join fetch o.table t
            left join fetch o.customer c
            left join fetch o.orderItems oi
            left join fetch oi.menuItem mi
            where t.id = :tableId
              and o.closedAt is null
              and o.status <> org.example.goldenheartrestaurant.modules.order.entity.OrderStatus.CANCELLED
            order by o.createdAt desc
            """)
    List<Order> findActiveOrdersByTableId(@Param("tableId") Integer tableId);
}
