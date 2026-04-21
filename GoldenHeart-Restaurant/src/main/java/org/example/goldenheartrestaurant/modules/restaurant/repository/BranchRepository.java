package org.example.goldenheartrestaurant.modules.restaurant.repository;

import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository cơ bản của Branch.
 *
 * Branch là dữ liệu tham chiếu xuất hiện ở nhiều module:
 * menu, inventory, order, employee profile...
 * Nên hiện tại chỉ cần CRUD chuẩn để các service khác tra cứu.
 */
public interface BranchRepository extends JpaRepository<Branch, Integer> {

    @Query("""
            select b
            from Branch b
            join fetch b.restaurant r
            where (:restaurantId is null or r.id = :restaurantId)
              and (:keyword is null
                   or lower(b.name) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(b.address, '')) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(b.phone, '')) like lower(concat('%', :keyword, '%'))
                   or lower(r.name) like lower(concat('%', :keyword, '%')))
            order by r.name asc, b.name asc
            """)
    List<Branch> findAllForListing(@Param("restaurantId") Integer restaurantId,
                                   @Param("keyword") String keyword);

    @Query("""
            select b
            from Branch b
            join fetch b.restaurant
            where b.id = :branchId
            """)
    Optional<Branch> findDetailById(@Param("branchId") Integer branchId);

    boolean existsByRestaurant_IdAndNameIgnoreCase(Integer restaurantId, String name);

    boolean existsByRestaurant_IdAndNameIgnoreCaseAndIdNot(Integer restaurantId, String name, Integer id);
}
