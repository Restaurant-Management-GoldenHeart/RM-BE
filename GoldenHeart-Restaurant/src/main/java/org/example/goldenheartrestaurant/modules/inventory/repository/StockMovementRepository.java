package org.example.goldenheartrestaurant.modules.inventory.repository;

import org.example.goldenheartrestaurant.modules.inventory.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository log biến động kho.
 *
 * StockMovement khác InventoryActionLog ở chỗ:
 * - InventoryActionLog nghiêng về audit thao tác người dùng
 * - StockMovement nghiêng về biến động số lượng / giá vốn trong kho
 */
public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {

    /**
     * Dùng để chặn thay đổi có thể làm sai lịch sử kho,
     * ví dụ đổi đơn vị đo hoặc xóa ingredient khi đã có movement.
     */
    boolean existsByIngredientId(Integer ingredientId);
}
