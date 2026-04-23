package org.example.goldenheartrestaurant.modules.menu.repository;

import org.example.goldenheartrestaurant.modules.menu.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository cơ bản của danh mục món ăn.
 *
 * Hiện tại Category chủ yếu đóng vai trò dữ liệu tham chiếu cho MenuItem,
 * nên chỉ cần các hàm CRUD mặc định của JpaRepository là đủ.
 */
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    @Query("""
            select c
            from Category c
            where (:keyword is null
                   or lower(c.name) like lower(concat('%', :keyword, '%'))
                   or lower(c.description) like lower(concat('%', :keyword, '%')))
            """)
    Page<Category> search(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            select distinct c
            from Category c
            left join fetch c.menuItems
            where c.id = :categoryId
            """)
    Optional<Category> findDetailById(@Param("categoryId") Integer categoryId);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}
