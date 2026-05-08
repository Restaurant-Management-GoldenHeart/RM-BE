package org.example.goldenheartrestaurant.modules.restaurant.repository;

import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTable;
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

    /**
     * Lấy danh sách bàn theo branchId, status và keyword tìm kiếm.
     *
     * Dùng nativeQuery = true để tránh lỗi Hibernate 7 + MySQL: Hibernate 7 tự thêm
     * `escape ''` vào mệnh đề LIKE (invalid syntax trong MySQL), gây 500 lỗi khi gọi
     * API danh sách bàn. Native SQL không đi qua JPQL parser nên không bị ảnh hưởng.
     */
    @Query("""
            select t
            from RestaurantTable t
            join fetch t.branch b
            left join fetch t.area a
            order by b.name asc,
                     coalesce(a.displayOrder, 999999) asc,
                     coalesce(t.displayOrder, 999999) asc,
                     lower(t.tableNumber) asc
            """)
    List<RestaurantTable> findAllForListingBase();

    @Query("""
            select t
            from RestaurantTable t
            join fetch t.branch b
            left join fetch t.area a
            where b.id = :branchId
            order by b.name asc,
                     coalesce(a.displayOrder, 999999) asc,
                     coalesce(t.displayOrder, 999999) asc,
                     lower(t.tableNumber) asc
            """)
    List<RestaurantTable> findAllForListingBaseByBranchId(@Param("branchId") Integer branchId);

    @Query("""
            select t
            from RestaurantTable t
            join fetch t.branch
            left join fetch t.area
            where t.id = :tableId
            """)
    Optional<RestaurantTable> findDetailById(@Param("tableId") Integer tableId);

    @Query("""
            select t
            from RestaurantTable t
            join fetch t.branch b
            left join fetch t.area a
            where t.id in :rootIds
               or t.mergedIntoTableId in :rootIds
            order by coalesce(t.displayOrder, 999999) asc, t.tableNumber asc
            """)
    List<RestaurantTable> findAllInMergedGroups(@Param("rootIds") List<Integer> rootIds);

    @Query("""
            select t
            from RestaurantTable t
            join fetch t.branch
            left join fetch t.area
            where t.mergedIntoTableId = :rootTableId
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
