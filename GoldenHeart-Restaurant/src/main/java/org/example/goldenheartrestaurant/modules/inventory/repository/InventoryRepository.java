package org.example.goldenheartrestaurant.modules.inventory.repository;

import jakarta.persistence.LockModeType;
import org.example.goldenheartrestaurant.modules.inventory.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository của Inventory.
 *
 * Đây là repository quan trọng vì nó phục vụ cả:
 * - màn hình danh sách tồn kho
 * - low stock alerts
 * - lock tồn kho khi kitchen complete món
 */
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    @Query(
            value = """
                    select i
                    from Inventory i
                    join fetch i.branch b
                    join fetch i.ingredient ing
                    left join fetch ing.measurementUnit mu
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
     * Query tìm kiếm chính cho màn hình danh sách inventory.
     *
     * Lý do phải join fetch:
     * - FE luôn cần tên branch, tên ingredient và đơn vị tính
     * - nếu không fetch sẵn sẽ rất dễ sinh ra N+1 query khi map response
     *
     * Lý do có countQuery riêng:
     * - search đang trả Page nên cần câu count đơn giản hơn
     * - tránh để Hibernate tự sinh count query phức tạp và kém hiệu năng
     */
    Page<Inventory> search(
            @Param("keyword") String keyword,
            @Param("branchId") Integer branchId,
            @Param("lowStockOnly") boolean lowStockOnly,
            Pageable pageable
    );

    /**
     * Dùng khóa pessimistic write khi trừ kho trong bếp.
     *
     * Mục tiêu:
     * - tránh 2 request hoàn thành món cùng lúc cùng trừ một nguyên liệu
     * - đảm bảo số tồn được đọc và cập nhật một cách tuần tự
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i
            from Inventory i
            join fetch i.branch
            join fetch i.ingredient ing
            left join fetch ing.measurementUnit
            where i.branch.id = :branchId
              and i.ingredient.id in :ingredientIds
            """)
    List<Inventory> findAllForUpdateByBranchIdAndIngredientIds(
            @Param("branchId") Integer branchId,
            @Param("ingredientIds") List<Integer> ingredientIds
    );

    /**
     * Lấy chi tiết inventory để trả API detail.
     * Fetch sẵn branch + ingredient + measurement unit để map DTO trọn vẹn.
     */
    @Query("""
            select i
            from Inventory i
            join fetch i.branch b
            join fetch i.ingredient ing
            left join fetch ing.measurementUnit
            where i.id = :inventoryId
            """)
    Optional<Inventory> findDetailById(@Param("inventoryId") Integer inventoryId);

    /**
     * Đảm bảo mỗi chi nhánh chỉ có đúng một dòng tồn kho cho một nguyên liệu.
     */
    Optional<Inventory> findByBranchIdAndIngredientId(Integer branchId, Integer ingredientId);

    /**
     * Trả danh sách cảnh báo tồn kho thấp để FE hiển thị alert.
     * Chỉ lấy những item có minStockLevel và quantity đã chạm/ngã xuống ngưỡng.
     */
    @Query("""
            select i
            from Inventory i
            join fetch i.branch b
            join fetch i.ingredient ing
            left join fetch ing.measurementUnit mu
            where (:branchId is null or b.id = :branchId)
              and i.minStockLevel is not null
              and coalesce(i.quantity, 0) <= i.minStockLevel
            order by i.quantity asc, ing.name asc
            """)
    List<Inventory> findLowStockAlerts(@Param("branchId") Integer branchId);
}
