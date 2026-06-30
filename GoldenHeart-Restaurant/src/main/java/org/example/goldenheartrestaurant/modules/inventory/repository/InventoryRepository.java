package org.example.goldenheartrestaurant.modules.inventory.repository;

import org.example.goldenheartrestaurant.modules.inventory.entity.Inventory;
import org.example.goldenheartrestaurant.modules.inventory.repository.projection.InventorySummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository cá»§a Inventory.
 *
 * ÄÃ¢y lÃ  repository quan trá»ng vÃ¬ nÃ³ phá»¥c vá»¥ cáº£:
 * - mÃ n hÃ¬nh danh sÃ¡ch tá»“n kho
 * - low stock alerts
 * - lock tá»“n kho khi kitchen complete mÃ³n
 */
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    boolean existsByBranchId(Integer branchId);

    @Query("""
            select
                count(i) as totalItems,
                coalesce(sum(coalesce(i.quantity, 0)), 0) as totalQuantity,
                coalesce(sum(coalesce(i.quantity, 0) * coalesce(i.averageUnitCost, 0)), 0) as totalInventoryValue,
                coalesce(sum(case
                    when i.minStockLevel is not null and coalesce(i.quantity, 0) <= i.minStockLevel then 1
                    else 0
                end), 0) as lowStockCount,
                coalesce(sum(case
                    when coalesce(i.quantity, 0) = 0 then 1
                    else 0
                end), 0) as outOfStockCount
            from Inventory i
            where (:branchId is null or i.branch.id = :branchId)
            """)
    InventorySummaryProjection summarizeCurrentStateByBranch(@Param("branchId") Integer branchId);

    @Query(
            value = """
                    select i
                    from Inventory i
                    join fetch i.branch b
                    join fetch i.ingredient ing
                    left join fetch ing.measurementUnit mu
                    left join fetch ing.defaultPurchaseUnit dpu
                    where (:keyword is null
                           or lower(ing.name) like lower(concat('%', :keyword, '%'))
                           or lower(b.name) like lower(concat('%', :keyword, '%'))
                           or lower(mu.name) like lower(concat('%', :keyword, '%'))
                           or lower(mu.symbol) like lower(concat('%', :keyword, '%')))
                      and (:branchId is null or b.id = :branchId)
                      and (:lowStockOnly = false
                           or (i.minStockLevel is not null and coalesce(i.quantity, 0) <= i.minStockLevel))
                    """,
            countQuery = """
                    select count(i)
                    from Inventory i
                    join i.branch b
                    join i.ingredient ing
                    left join ing.measurementUnit mu
                    where (:keyword is null
                           or lower(ing.name) like lower(concat('%', :keyword, '%'))
                           or lower(b.name) like lower(concat('%', :keyword, '%'))
                           or lower(mu.name) like lower(concat('%', :keyword, '%'))
                           or lower(mu.symbol) like lower(concat('%', :keyword, '%')))
                      and (:branchId is null or b.id = :branchId)
                      and (:lowStockOnly = false
                           or (i.minStockLevel is not null and coalesce(i.quantity, 0) <= i.minStockLevel))
                    """
    )
    /**
     * Query tÃ¬m kiáº¿m chÃ­nh cho mÃ n hÃ¬nh danh sÃ¡ch inventory.
     *
     * LÃ½ do pháº£i join fetch:
     * - FE luÃ´n cáº§n tÃªn branch, tÃªn ingredient vÃ  Ä‘Æ¡n vá»‹ tÃ­nh
     * - náº¿u khÃ´ng fetch sáºµn sáº½ ráº¥t dá»… sinh ra N+1 query khi map response
     *
     * LÃ½ do cÃ³ countQuery riÃªng:
     * - search Ä‘ang tráº£ Page nÃªn cáº§n cÃ¢u count Ä‘Æ¡n giáº£n hÆ¡n
     * - trÃ¡nh Ä‘á»ƒ Hibernate tá»± sinh count query phá»©c táº¡p vÃ  kÃ©m hiá»‡u nÄƒng
     */
    Page<Inventory> search(
            @Param("keyword") String keyword,
            @Param("branchId") Integer branchId,
            @Param("lowStockOnly") boolean lowStockOnly,
            Pageable pageable
    );

    /**
     * DÃ¹ng khÃ³a pessimistic write khi trá»« kho trong báº¿p.
     *
     * Má»¥c tiÃªu:
     * - trÃ¡nh 2 request hoÃ n thÃ nh mÃ³n cÃ¹ng lÃºc cÃ¹ng trá»« má»™t nguyÃªn liá»‡u
     * - Ä‘áº£m báº£o sá»‘ tá»“n Ä‘Æ°á»£c Ä‘á»c vÃ  cáº­p nháº­t má»™t cÃ¡ch tuáº§n tá»±
     */
    @Query(value = """
            SELECT i.* FROM inventory i
            JOIN branches b ON b.id = i.branch_id
            JOIN ingredients ing ON ing.id = i.ingredient_id
            WHERE i.deleted_at IS NULL
              AND i.branch_id = :branchId
              AND i.ingredient_id IN (:ingredientIds)
            FOR UPDATE
            """, nativeQuery = true)
    List<Inventory> findAllForUpdateByBranchIdAndIngredientIds(
            @Param("branchId") Integer branchId,
            @Param("ingredientIds") List<Integer> ingredientIds
    );

    /**
     * Láº¥y chi tiáº¿t inventory Ä‘á»ƒ tráº£ API detail.
     * Fetch sáºµn branch + ingredient + measurement unit Ä‘á»ƒ map DTO trá»n váº¹n.
     */
    @Query("""
            select i
            from Inventory i
            join fetch i.branch b
            join fetch i.ingredient ing
            left join fetch ing.measurementUnit
            left join fetch ing.defaultPurchaseUnit
            where i.id = :inventoryId
            """)
    Optional<Inventory> findDetailById(@Param("inventoryId") Integer inventoryId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i
            from Inventory i
            join fetch i.branch b
            join fetch i.ingredient ing
            left join fetch ing.measurementUnit
            left join fetch ing.defaultPurchaseUnit
            where i.id = :inventoryId
            """)
    Optional<Inventory> findDetailForUpdateById(@Param("inventoryId") Integer inventoryId);

    /**
     * Äáº£m báº£o má»—i chi nhÃ¡nh chá»‰ cÃ³ Ä‘Ãºng má»™t dÃ²ng tá»“n kho cho má»™t nguyÃªn liá»‡u.
     */
    Optional<Inventory> findByBranchIdAndIngredientId(Integer branchId, Integer ingredientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i
            from Inventory i
            join fetch i.branch
            join fetch i.ingredient ing
            left join fetch ing.measurementUnit
            left join fetch ing.defaultPurchaseUnit
            where i.branch.id = :branchId
              and i.ingredient.id = :ingredientId
            """)
    Optional<Inventory> findForUpdateByBranchIdAndIngredientId(
            @Param("branchId") Integer branchId,
            @Param("ingredientId") Integer ingredientId
    );

    @Query("""
            select i
            from Inventory i
            join fetch i.ingredient ing
            left join fetch ing.measurementUnit
            left join fetch ing.defaultPurchaseUnit
            where i.branch.id = :branchId
              and i.ingredient.id in :ingredientIds
            """)
    List<Inventory> findAllByBranchIdAndIngredientIds(
            @Param("branchId") Integer branchId,
            @Param("ingredientIds") List<Integer> ingredientIds
    );

    /**
     * Tráº£ danh sÃ¡ch cáº£nh bÃ¡o tá»“n kho tháº¥p Ä‘á»ƒ FE hiá»ƒn thá»‹ alert.
     * Chá»‰ láº¥y nhá»¯ng item cÃ³ minStockLevel vÃ  quantity Ä‘Ã£ cháº¡m/ngÃ£ xuá»‘ng ngÆ°á»¡ng.
     */
    @Query("""
            select i
            from Inventory i
            join fetch i.branch b
            join fetch i.ingredient ing
            left join fetch ing.measurementUnit mu
            left join fetch ing.defaultPurchaseUnit dpu
            where (:branchId is null or b.id = :branchId)
              and i.minStockLevel is not null
              and coalesce(i.quantity, 0) <= i.minStockLevel
            order by i.quantity asc, ing.name asc
            """)
    List<Inventory> findLowStockAlerts(@Param("branchId") Integer branchId);
}


