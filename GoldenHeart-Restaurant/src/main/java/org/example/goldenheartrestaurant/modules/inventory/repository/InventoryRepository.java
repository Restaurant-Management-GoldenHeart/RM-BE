package org.example.goldenheartrestaurant.modules.inventory.repository;

import jakarta.persistence.LockModeType;
import org.example.goldenheartrestaurant.modules.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i
            from Inventory i
            join fetch i.branch
            join fetch i.ingredient
            where i.branch.id = :branchId
              and i.ingredient.id in :ingredientIds
            """)
    List<Inventory> findAllForUpdateByBranchIdAndIngredientIds(
            @Param("branchId") Integer branchId,
            @Param("ingredientIds") List<Integer> ingredientIds
    );

    Optional<Inventory> findByBranchIdAndIngredientId(Integer branchId, Integer ingredientId);
}
