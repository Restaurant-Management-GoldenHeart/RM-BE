package org.example.goldenheartrestaurant.modules.menu.repository;

import org.example.goldenheartrestaurant.modules.menu.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository của MenuItem.
 *
 * Điểm đáng chú ý:
 * - search dùng EntityGraph để kéo luôn branch/category, tránh N+1 query
 * - detail query fetch luôn recipes + ingredients để response đủ dữ liệu một lần
 */
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {

    boolean existsByBranchId(Integer branchId);

    boolean existsByCategoryId(Integer categoryId);

    /**
     * Search chính cho màn hình danh sách menu.
     *
     * Dùng EntityGraph để kéo branch/category cùng một lượt,
     * vì response danh sách luôn cần 2 thông tin này.
     */
    @EntityGraph(attributePaths = {"branch", "category"})
    @Query("""
            select mi
            from MenuItem mi
            where (:keyword is null
                   or lower(mi.name) like lower(concat('%', :keyword, '%'))
                   or lower(mi.description) like lower(concat('%', :keyword, '%')))
              and (:branchId is null or mi.branch.id = :branchId)
              and (:categoryId is null or mi.category.id = :categoryId)
            """)
    Page<MenuItem> search(
            @Param("keyword") String keyword,
            @Param("branchId") Integer branchId,
            @Param("categoryId") Integer categoryId,
            Pageable pageable
    );

    /**
     * Lấy detail một món kèm toàn bộ recipe.
     *
     * join fetch ở đây rất quan trọng vì FE cần hiển thị luôn
     * danh sách ingredient + quantity của từng thành phần.
     */
    @Query("""
            select distinct mi
            from MenuItem mi
            join fetch mi.branch
            join fetch mi.category
            left join fetch mi.recipes r
            left join fetch r.ingredient
            where mi.id = :menuItemId
            """)
    Optional<MenuItem> findDetailById(@Param("menuItemId") Integer menuItemId);

    /**
     * Kiểm tra trùng tên món trong cùng branch + category khi create.
     */
    boolean existsByBranchIdAndCategoryIdAndNameIgnoreCase(Integer branchId, Integer categoryId, String name);

    /**
     * Kiểm tra trùng tên món trong cùng branch + category khi update,
     * nhưng bỏ qua chính bản ghi hiện tại.
     */
    boolean existsByBranchIdAndCategoryIdAndNameIgnoreCaseAndIdNot(Integer branchId, Integer categoryId, String name, Integer id);

    @Query("""
            select count(mi)
            from MenuItem mi
            where (:branchId is null or mi.branch.id = :branchId)
            """)
    long countForReport(@Param("branchId") Integer branchId);

    /**
     * Lấy danh sách món ăn phổ biến cho homepage — gộp theo TÊN MÓN xuyên suốt tất cả chi nhánh.
     *
     * <p><b>Logic nghiệp vụ:</b>
     * <ul>
     *   <li>Cùng tên món ở nhiều chi nhánh → gộp thành 1 dòng, cộng dồn số lượt gọi.</li>
     *   <li>Chỉ tính đơn hàng không bị huỷ (status != 'CANCELLED').</li>
     *   <li>Món chưa từng được đặt vẫn hiện (LEFT JOIN), totalOrderCount = 0.</li>
     *   <li>Chỉ lấy món đang AVAILABLE.</li>
     *   <li>Sắp xếp: gọi nhiều nhất → đầu tiên; nếu bằng nhau → sort theo tên.</li>
     * </ul>
     *
     * <p>Native query dùng alias trong ORDER BY (MySQL hỗ trợ) để tránh lặp lại biểu thức CASE SUM.
     */
    @Query(value = """
            SELECT MIN(mi.id)              AS id,
                   mi.name                 AS name,
                   MIN(mi.image_url)       AS imageUrl,
                   MIN(mi.description)     AS description,
                   MIN(mi.price)           AS price,
                   cat.id                  AS categoryId,
                   cat.name                AS categoryName,
                   cat.production_station  AS productionStation,
                   COALESCE(SUM(
                       CASE WHEN o.status IS NOT NULL AND o.status != 'CANCELLED'
                            THEN oi.quantity
                            ELSE 0
                       END
                   ), 0)                   AS totalOrderCount
            FROM menu_items mi
            JOIN categories cat ON mi.category_id = cat.id
            LEFT JOIN order_items oi ON oi.menu_item_id = mi.id
            LEFT JOIN orders o ON oi.order_id = o.id
            WHERE mi.status = 'AVAILABLE'
            GROUP BY mi.name, cat.id, cat.name, cat.production_station
            ORDER BY totalOrderCount DESC, mi.name ASC
            """, nativeQuery = true)
    List<PopularDishProjection> findPopularDishesForHomepage();
}
