package org.example.goldenheartrestaurant.modules.order.repository;

import org.example.goldenheartrestaurant.modules.menu.entity.ProductionStation;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItem;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItemStatus;
import org.example.goldenheartrestaurant.modules.report.repository.projection.OrderItemStatusCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository cá»§a OrderItem cho luá»“ng báº¿p.
 *
 * Query detail fetch sáºµn:
 * - order
 * - branch
 * - menu item
 * - recipes
 * - ingredients
 *
 * Ä‘á»ƒ service complete mÃ³n khÃ´ng bá»‹ N+1 query khi trá»« kho.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    /**
     * Láº¥y Ä‘áº§y Ä‘á»§ dá»¯ liá»‡u cáº§n cho báº¿p hoÃ n thÃ nh mÃ³n.
     *
     * Cáº§n fetch toÃ n bá»™ menu item + recipe + ingredient ngay táº¡i Ä‘Ã¢y
     * Ä‘á»ƒ service cÃ³ thá»ƒ:
     * - biáº¿t mÃ³n gá»“m nhá»¯ng nguyÃªn liá»‡u nÃ o
     * - tÃ­nh Ä‘Ãºng lÆ°á»£ng cáº§n trá»« kho
     * - trÃ¡nh phÃ¡t sinh thÃªm nhiá»u query trong transaction
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
            join fetch mi.category c
            where oi.status in :statuses
              and (:branchId is null or b.id = :branchId)
              and (:productionStation is null or c.productionStation = :productionStation)
            order by oi.status asc, o.createdAt asc, oi.id asc
            """)
    List<OrderItem> findKitchenItemsByStatuses(
            @Param("statuses") Collection<OrderItemStatus> statuses,
            @Param("branchId") Integer branchId,
            @Param("productionStation") ProductionStation productionStation
    );

    @Query("""
            select oi.status as status, count(oi) as total
            from OrderItem oi
            join oi.order o
            where oi.status in :statuses
              and (:branchId is null or o.branch.id = :branchId)
            group by oi.status
            """)
    List<OrderItemStatusCountProjection> countKitchenItemsByStatusesForReport(
            @Param("statuses") Collection<OrderItemStatus> statuses,
            @Param("branchId") Integer branchId
    );
}