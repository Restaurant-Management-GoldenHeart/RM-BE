package org.example.goldenheartrestaurant.modules.menu.repository;

import org.example.goldenheartrestaurant.modules.menu.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {

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

    boolean existsByBranchIdAndCategoryIdAndNameIgnoreCase(Integer branchId, Integer categoryId, String name);

    boolean existsByBranchIdAndCategoryIdAndNameIgnoreCaseAndIdNot(Integer branchId, Integer categoryId, String name, Integer id);
}
