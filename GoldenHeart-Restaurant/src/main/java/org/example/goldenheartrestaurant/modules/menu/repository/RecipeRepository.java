package org.example.goldenheartrestaurant.modules.menu.repository;

import org.example.goldenheartrestaurant.modules.menu.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository của Recipe.
 *
 * Recipe là cầu nối giữa menu item và ingredient,
 * nên các method ở đây chủ yếu phục vụ:
 * - thay recipe khi update món
 * - kiểm tra ingredient có đang được dùng trong recipe hay không
 */
public interface RecipeRepository extends JpaRepository<Recipe, Integer> {

    /**
     * Xóa toàn bộ recipe cũ của một món trước khi ghi recipe mới.
     *
     * Làm như vậy giúp tránh va chạm unique constraint
     * khi cùng một ingredient được thay định mức trong lần update.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Recipe r where r.menuItem.id = :menuItemId")
    void deleteByMenuItemId(@Param("menuItemId") Integer menuItemId);

    /**
     * Kiểm tra ingredient có đang được ít nhất một recipe sử dụng không.
     * Dùng khi cân nhắc sửa/xóa ingredient master.
     */
    @Query("""
            select (count(r) > 0)
            from Recipe r
            where r.ingredient.id = :ingredientId
            """)
    boolean existsByIngredientId(@Param("ingredientId") Integer ingredientId);

    /**
     * Kiểm tra ingredient có đang được recipe ở một chi nhánh cụ thể sử dụng không.
     * Dùng cho rule inventory: không xóa inventory item nếu branch đó vẫn còn món đang dùng nguyên liệu này.
     */
    @Query("""
            select (count(r) > 0)
            from Recipe r
            where r.ingredient.id = :ingredientId
              and r.menuItem.branch.id = :branchId
            """)
    boolean existsByIngredientIdAndBranchId(
            @Param("ingredientId") Integer ingredientId,
            @Param("branchId") Integer branchId
    );
}
