package org.example.goldenheartrestaurant.modules.restaurant.repository;

import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTable;
import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTableStatus;
import org.example.goldenheartrestaurant.modules.report.repository.projection.TableStatusCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Integer> {

    boolean existsByBranch_Id(Integer branchId);

    boolean existsByArea_Id(Integer areaId);

    boolean existsByBranch_IdAndTableNumberIgnoreCase(Integer branchId, String tableNumber);

    boolean existsByBranch_IdAndTableNumberIgnoreCaseAndIdNot(Integer branchId, String tableNumber, Integer id);

    boolean existsByMergedIntoTable_Id(Integer rootTableId);

    @Query("""
            select t
            from RestaurantTable t
            join fetch t.branch b
            left join fetch t.area a
            left join fetch t.mergedIntoTable m
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
            left join fetch t.mergedIntoTable
            where t.id = :tableId
            """)
    Optional<RestaurantTable> findDetailById(@Param("tableId") Integer tableId);

    @Query("""
            select t
            from RestaurantTable t
            join fetch t.branch b
            left join fetch t.area a
            left join fetch t.mergedIntoTable m
            where t.id in :rootIds
               or m.id in :rootIds
            order by coalesce(t.displayOrder, 999999) asc, t.tableNumber asc
            """)
    List<RestaurantTable> findAllInMergedGroups(@Param("rootIds") List<Integer> rootIds);

    @Query("""
            select t
            from RestaurantTable t
            join fetch t.branch
            left join fetch t.area
            left join fetch t.mergedIntoTable
            where t.mergedIntoTable.id = :rootTableId
            order by coalesce(t.displayOrder, 999999) asc, t.tableNumber asc
            """)
    List<RestaurantTable> findMergedMembersByRootTableId(@Param("rootTableId") Integer rootTableId);

    @Query("""
            select t.status as status, count(t) as total
            from RestaurantTable t
            where (:branchId is null or t.branch.id = :branchId)
            group by t.status
            """)
    List<TableStatusCountProjection> countByStatusForReport(@Param("branchId") Integer branchId);
}
