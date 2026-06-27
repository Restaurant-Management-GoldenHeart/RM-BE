package org.example.goldenheartrestaurant.modules.combo.repository;

import org.example.goldenheartrestaurant.modules.combo.entity.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ComboRepository extends JpaRepository<Combo, Integer> {

    @Query("""
            select distinct c from Combo c
            join fetch c.branch
            left join fetch c.items ci
            left join fetch ci.menuItem
            where c.branch.id = :branchId
            """)
    List<Combo> findByBranchIdWithItems(@Param("branchId") Integer branchId);

    @Query("""
            select distinct c from Combo c
            join fetch c.branch
            left join fetch c.items ci
            left join fetch ci.menuItem
            where c.id = :id
            """)
    Optional<Combo> findByIdWithItems(@Param("id") Integer id);

    boolean existsByBranchIdAndNameIgnoreCase(Integer branchId, String name);

    boolean existsByBranchIdAndNameIgnoreCaseAndIdNot(Integer branchId, String name, Integer id);
}
