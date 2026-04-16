package org.example.goldenheartrestaurant.modules.order.repository;

import org.example.goldenheartrestaurant.modules.order.entity.OrderItem;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
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
            left join fetch o.table
            join fetch oi.menuItem mi
            left join fetch mi.recipes r
            left join fetch r.ingredient
            where oi.id = :orderItemId
            """)
    Optional<OrderItem> findKitchenDetailById(@Param("orderItemId") Integer orderItemId);

    @Query("""
            select oi
            from OrderItem oi
            join fetch oi.order o
            join fetch o.branch b
            left join fetch o.table t
            join fetch oi.menuItem mi
            where oi.status in :statuses
              and (:branchId is null or b.id = :branchId)
            order by oi.status asc, o.createdAt asc, oi.id asc
            """)
    List<OrderItem> findKitchenItemsByStatuses(
            @Param("statuses") Collection<OrderItemStatus> statuses,
            @Param("branchId") Integer branchId
    );
}
