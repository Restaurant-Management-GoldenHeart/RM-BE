package org.example.goldenheartrestaurant.modules.restaurant.repository;

import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTable;
import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Integer> {

    boolean existsByBranch_IdAndTableNumberIgnoreCase(Integer branchId, String tableNumber);

    boolean existsByBranch_IdAndTableNumberIgnoreCaseAndIdNot(Integer branchId, String tableNumber, Integer id);

    @Query("""
            select t
            from RestaurantTable t
            join fetch t.branch b
            left join fetch t.area a
            where (:branchId is null or b.id = :branchId)
              and (:status is null or t.status = :status)
              and (:keyword is null
                   or lower(t.tableNumber) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(a.name, '')) like lower(concat('%', :keyword, '%')))
            order by b.name asc,
                     coalesce(a.displayOrder, 999999) asc,
                     coalesce(t.displayOrder, 999999) asc,
                     t.tableNumber asc
            """)
    List<RestaurantTable> findAllForListing(@Param("branchId") Integer branchId,
                                            @Param("status") RestaurantTableStatus status,
                                            @Param("keyword") String keyword);

    @Query("""
            select t
            from RestaurantTable t
            join fetch t.branch
            left join fetch t.area
            where t.id = :tableId
            """)
    Optional<RestaurantTable> findDetailById(@Param("tableId") Integer tableId);
}
