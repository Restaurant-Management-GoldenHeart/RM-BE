package org.example.goldenheartrestaurant.modules.inventory.repository;

import org.example.goldenheartrestaurant.modules.inventory.entity.StockMovement;
import org.example.goldenheartrestaurant.modules.inventory.repository.projection.InventoryMovementPeriodProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    @Query("""
            select coalesce(sum(sm.totalCost), 0)
            from StockMovement sm
            where sm.order.id = :orderId
            """)
    BigDecimal sumTotalCostByOrderId(@Param("orderId") Integer orderId);

    @Query("""
            select
                function('date_format', sm.occurredAt, '%Y-%m-%d') as periodKey,
                coalesce(sum(case when sm.movementType = org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType.RECEIPT_IN then sm.totalCost else 0 end), 0) as receiptValue,
                coalesce(sum(case when sm.movementType = org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType.SALE_OUT then sm.totalCost else 0 end), 0) as saleValue,
                coalesce(sum(case when sm.movementType = org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType.WASTE_OUT then sm.totalCost else 0 end), 0) as wasteValue,
                coalesce(sum(case when sm.movementType = org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType.ADJUSTMENT_IN then sm.totalCost else 0 end), 0) as adjustmentInValue,
                coalesce(sum(case when sm.movementType = org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType.ADJUSTMENT_OUT then sm.totalCost else 0 end), 0) as adjustmentOutValue,
                coalesce(sum(case when sm.movementType = org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType.STOCKTAKE_IN then sm.totalCost else 0 end), 0) as stocktakeInValue,
                coalesce(sum(case when sm.movementType = org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType.STOCKTAKE_OUT then sm.totalCost else 0 end), 0) as stocktakeOutValue,
                coalesce(sum(case when sm.movementType = org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType.RETURN_OUT then sm.totalCost else 0 end), 0) as returnOutValue
            from StockMovement sm
            where (:branchId is null or sm.branch.id = :branchId)
              and sm.occurredAt >= :fromDateTime
              and sm.occurredAt < :toDateTime
            group by function('date_format', sm.occurredAt, '%Y-%m-%d')
            order by function('date_format', sm.occurredAt, '%Y-%m-%d')
            """)
    List<InventoryMovementPeriodProjection> summarizeMovementByDay(@Param("branchId") Integer branchId,
                                                                   @Param("fromDateTime") LocalDateTime fromDateTime,
                                                                   @Param("toDateTime") LocalDateTime toDateTime);

    @Query("""
            select
                function('date_format', sm.occurredAt, '%Y-%m') as periodKey,
                coalesce(sum(case when sm.movementType = org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType.RECEIPT_IN then sm.totalCost else 0 end), 0) as receiptValue,
                coalesce(sum(case when sm.movementType = org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType.SALE_OUT then sm.totalCost else 0 end), 0) as saleValue,
                coalesce(sum(case when sm.movementType = org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType.WASTE_OUT then sm.totalCost else 0 end), 0) as wasteValue,
                coalesce(sum(case when sm.movementType = org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType.ADJUSTMENT_IN then sm.totalCost else 0 end), 0) as adjustmentInValue,
                coalesce(sum(case when sm.movementType = org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType.ADJUSTMENT_OUT then sm.totalCost else 0 end), 0) as adjustmentOutValue,
                coalesce(sum(case when sm.movementType = org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType.STOCKTAKE_IN then sm.totalCost else 0 end), 0) as stocktakeInValue,
                coalesce(sum(case when sm.movementType = org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType.STOCKTAKE_OUT then sm.totalCost else 0 end), 0) as stocktakeOutValue,
                coalesce(sum(case when sm.movementType = org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType.RETURN_OUT then sm.totalCost else 0 end), 0) as returnOutValue
            from StockMovement sm
            where (:branchId is null or sm.branch.id = :branchId)
              and sm.occurredAt >= :fromDateTime
              and sm.occurredAt < :toDateTime
            group by function('date_format', sm.occurredAt, '%Y-%m')
            order by function('date_format', sm.occurredAt, '%Y-%m')
            """)
    List<InventoryMovementPeriodProjection> summarizeMovementByMonth(@Param("branchId") Integer branchId,
                                                                     @Param("fromDateTime") LocalDateTime fromDateTime,
                                                                     @Param("toDateTime") LocalDateTime toDateTime);
}
