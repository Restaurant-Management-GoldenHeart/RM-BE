package org.example.goldenheartrestaurant.modules.order.repository;

import org.example.goldenheartrestaurant.modules.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    @Query("""
            select distinct oi
            from OrderItem oi
            join fetch oi.order o
            join fetch o.branch
            join fetch oi.menuItem mi
            left join fetch mi.recipes r
            left join fetch r.ingredient
            where oi.id = :orderItemId
            """)
    Optional<OrderItem> findKitchenDetailById(@Param("orderItemId") Integer orderItemId);
}
