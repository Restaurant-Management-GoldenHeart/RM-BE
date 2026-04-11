package org.example.goldenheartrestaurant.modules.order.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.example.goldenheartrestaurant.modules.inventory.entity.Inventory;
import org.example.goldenheartrestaurant.modules.inventory.entity.StockMovement;
import org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType;
import org.example.goldenheartrestaurant.modules.inventory.repository.InventoryRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.StockMovementRepository;
import org.example.goldenheartrestaurant.modules.menu.entity.Recipe;
import org.example.goldenheartrestaurant.modules.order.dto.response.OrderItemCompletionResponse;
import org.example.goldenheartrestaurant.modules.order.dto.response.StockDeductionResponse;
import org.example.goldenheartrestaurant.modules.order.entity.Order;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItem;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItemStatus;
import org.example.goldenheartrestaurant.modules.order.entity.OrderStatus;
import org.example.goldenheartrestaurant.modules.order.repository.OrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
/**
 * Service xử lý luồng bếp hoàn thành món.
 *
 * Đây là một trong những luồng nhạy cảm nhất của hệ thống vì 1 thao tác complete món
 * phải giữ đồng thời 3 thứ luôn nhất quán:
 * - trạng thái order item
 * - tồn kho inventory
 * - lịch sử stock movement
 *
 * Vì vậy method chính được bọc trong @Transactional.
 */
public class KitchenProductionService {

    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderItemCompletionResponse completeOrderItem(Integer orderItemId, CustomUserDetails currentUser) {
        OrderItem orderItem = orderItemRepository.findKitchenDetailById(orderItemId)
                .orElseThrow(() -> new NotFoundException("Order item not found"));

        // Chặn các trạng thái không hợp lệ trước khi đụng vào tồn kho.
        if (orderItem.getStatus() == OrderItemStatus.CANCELLED) {
            throw new ConflictException("Cancelled order item cannot be completed");
        }
        if (orderItem.getStatus() == OrderItemStatus.COMPLETED || orderItem.getStatus() == OrderItemStatus.SERVED) {
            throw new ConflictException("Order item is already completed");
        }

        List<Recipe> recipes = orderItem.getMenuItem().getRecipes();
        if (recipes.isEmpty()) {
            // Không có recipe thì không thể biết phải trừ nguyên liệu nào.
            throw new ConflictException("Menu item has no recipe configured");
        }

        List<Integer> ingredientIds = recipes.stream()
                .map(recipe -> recipe.getIngredient().getId())
                .toList();

        Map<Integer, Inventory> inventoryMap = inventoryRepository
                .findAllForUpdateByBranchIdAndIngredientIds(orderItem.getOrder().getBranch().getId(), ingredientIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(inv -> inv.getIngredient().getId(), Function.identity()));
        // Query repository dùng pessimistic lock để tránh 2 request cùng lúc
        // trừ đè lên cùng một lượng tồn kho.

        User actor = userRepository.findEmployeeDetailById(currentUser.getUserId())
                .orElseThrow(() -> new NotFoundException("Current user not found"));

        List<StockDeductionResponse> deductions = new ArrayList<>();
        List<StockMovement> stockMovements = new ArrayList<>();

        for (Recipe recipe : recipes) {
            Inventory inventory = inventoryMap.get(recipe.getIngredient().getId());
            if (inventory == null) {
                throw new ConflictException("Inventory not found for ingredient: " + recipe.getIngredient().getName());
            }

            // Ví dụ:
            // recipe cần 0.20 kg / 1 món
            // orderItem.quantity = 3
            // => phải trừ 0.60 kg khỏi tồn kho
            BigDecimal quantityToDeduct = recipe.getQuantity().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
            BigDecimal currentStock = inventory.getQuantity() == null ? BigDecimal.ZERO : inventory.getQuantity();

            if (currentStock.compareTo(quantityToDeduct) < 0) {
                // Chỉ cần thiếu 1 ingredient là fail cả transaction,
                // không cho trừ kho dở dang nửa chừng.
                throw new ConflictException("Not enough stock for ingredient: " + recipe.getIngredient().getName());
            }

            BigDecimal remainingStock = currentStock.subtract(quantityToDeduct);
            BigDecimal unitCost = inventory.getAverageUnitCost() == null ? BigDecimal.ZERO : inventory.getAverageUnitCost();

            inventory.setQuantity(remainingStock);

            stockMovements.add(
                    StockMovement.builder()
                            .branch(orderItem.getOrder().getBranch())
                            .ingredient(recipe.getIngredient())
                            .order(orderItem.getOrder())
                            .orderItem(orderItem)
                            .createdBy(actor)
                            .movementType(StockMovementType.SALE_OUT)
                            .quantityChange(quantityToDeduct.negate())
                            .balanceAfter(remainingStock)
                            .unitCost(unitCost)
                            // Lưu lại giá vốn nguyên liệu tiêu hao của lần complete này.
                            .totalCost(unitCost.multiply(quantityToDeduct))
                            .occurredAt(LocalDateTime.now())
                            .note("Stock deducted after kitchen completed order item")
                            .build()
            );

            deductions.add(
                    new StockDeductionResponse(
                            recipe.getIngredient().getId(),
                            recipe.getIngredient().getName(),
                            recipe.getIngredient().resolveUnitSymbol(),
                            quantityToDeduct,
                            remainingStock
                    )
            );
        }

        inventoryRepository.saveAll(inventoryMap.values());
        stockMovementRepository.saveAll(stockMovements);

        orderItem.setStatus(OrderItemStatus.COMPLETED);
        updateOrderStatusAfterKitchenCompletion(orderItem.getOrder());

        orderItemRepository.save(orderItem);

        return new OrderItemCompletionResponse(
                orderItem.getId(),
                orderItem.getOrder().getId(),
                orderItem.getMenuItem().getName(),
                orderItem.getStatus().name(),
                deductions
        );
    }

    private void updateOrderStatusAfterKitchenCompletion(Order order) {
        boolean allDone = order.getOrderItems().stream()
                .allMatch(item -> item.getStatus() == OrderItemStatus.COMPLETED
                        || item.getStatus() == OrderItemStatus.SERVED
                        || item.getStatus() == OrderItemStatus.CANCELLED);

        // Chỉ khi mọi dòng con đã đi vào trạng thái cuối
        // thì order cha mới được lên COMPLETED.
        order.setStatus(allDone ? OrderStatus.COMPLETED : OrderStatus.PROCESSING);
    }
}
