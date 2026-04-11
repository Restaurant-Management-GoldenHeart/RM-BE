package org.example.goldenheartrestaurant.modules.order.repository;

import org.example.goldenheartrestaurant.modules.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository của OrderItem cho luồng bếp.
 *
 * Query detail fetch sẵn:
 * - order
 * - branch
 * - menu item
 * - recipes
 * - ingredients
 *
 * để service complete món không bị N+1 query khi trừ kho.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    /**
     * Lấy đầy đủ dữ liệu cần cho bếp hoàn thành món.
     *
     * Cần fetch toàn bộ menu item + recipe + ingredient ngay tại đây
     * để service có thể:
     * - biết món gồm những nguyên liệu nào
     * - tính đúng lượng cần trừ kho
     * - tránh phát sinh thêm nhiều query trong transaction
     */
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
