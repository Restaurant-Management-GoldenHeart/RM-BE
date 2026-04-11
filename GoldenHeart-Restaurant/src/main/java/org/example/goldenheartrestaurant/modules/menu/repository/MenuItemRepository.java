package org.example.goldenheartrestaurant.modules.menu.repository;

import org.example.goldenheartrestaurant.modules.menu.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository của MenuItem.
 *
 * Điểm đáng chú ý:
 * - search dùng EntityGraph để kéo luôn branch/category, tránh N+1 query
 * - detail query fetch luôn recipes + ingredients để response đủ dữ liệu một lần
 */
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {

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
}
