package org.example.goldenheartrestaurant.modules.inventory.repository;

import org.example.goldenheartrestaurant.modules.inventory.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository của Ingredient master.
 *
 * Ingredient là dữ liệu dùng chung cho:
 * - inventory
 * - recipe
 * - kitchen stock deduction
 */
public interface IngredientRepository extends JpaRepository<Ingredient, Integer> {

    /**
     * Tìm nguyên liệu theo tên, bỏ qua hoa thường.
     * Dùng khi create để tránh tạo trùng master ingredient.
     */
    Optional<Ingredient> findByBranchIdAndNameIgnoreCase(Integer branchId, String name);

    /**
     * Kiểm tra tên nguyên liệu có bị trùng với bản ghi khác hay không
     * trong luồng update.
     */
    boolean existsByBranchIdAndNameIgnoreCaseAndIdNot(Integer branchId, String name, Integer id);

    Optional<Ingredient> findByIdAndBranchId(Integer id, Integer branchId);

    /**
     * Dùng cho luồng backfill/migration để tìm những nguyên liệu cũ
     * chưa được gán measurement unit chuẩn.
     */
    List<Ingredient> findByMeasurementUnitIsNull();
}
