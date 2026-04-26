package org.example.goldenheartrestaurant.modules.restaurant.repository;

import org.example.goldenheartrestaurant.modules.restaurant.entity.DiningArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiningAreaRepository extends JpaRepository<DiningArea, Integer> {

    boolean existsByBranch_Id(Integer branchId);

    boolean existsByBranch_IdAndNameIgnoreCase(Integer branchId, String name);

    boolean existsByBranch_IdAndNameIgnoreCaseAndIdNot(Integer branchId, String name, Integer id);

    boolean existsByBranch_IdAndCodeIgnoreCase(Integer branchId, String code);

    boolean existsByBranch_IdAndCodeIgnoreCaseAndIdNot(Integer branchId, String code, Integer id);

    Optional<DiningArea> findByIdAndBranch_Id(Integer id, Integer branchId);

    @Query("""
            select distinct a
            from DiningArea a
            join fetch a.branch b
            left join fetch a.tables
            where (:branchId is null or b.id = :branchId)
              and (:active is null or a.active = :active)
              and (:keyword is null
                   or lower(a.name) like lower(concat('%', :keyword, '%'))
                   or lower(a.code) like lower(concat('%', :keyword, '%'))
                   or lower(b.name) like lower(concat('%', :keyword, '%')))
            order by b.name asc,
                     coalesce(a.displayOrder, 999999) asc,
                     a.name asc
            """)
    List<DiningArea> findAllForListing(@Param("branchId") Integer branchId,
                                       @Param("active") Boolean active,
                                       @Param("keyword") String keyword);

    @Query("""
            select distinct a
            from DiningArea a
            join fetch a.branch
            left join fetch a.tables
            where a.id = :areaId
            """)
    Optional<DiningArea> findDetailById(@Param("areaId") Integer areaId);
}
