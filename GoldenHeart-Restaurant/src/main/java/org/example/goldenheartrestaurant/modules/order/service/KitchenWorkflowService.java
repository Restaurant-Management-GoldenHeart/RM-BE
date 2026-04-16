
package org.example.goldenheartrestaurant.modules.order.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
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
import org.example.goldenheartrestaurant.modules.order.dto.response.KitchenPendingOrderItemResponse;
import org.example.goldenheartrestaurant.modules.order.dto.response.OrderItemCompletionResponse;
import org.example.goldenheartrestaurant.modules.order.dto.response.OrderItemStatusChangeResponse;
import org.example.goldenheartrestaurant.modules.order.dto.response.StockDeductionResponse;
import org.example.goldenheartrestaurant.modules.order.entity.Order;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItem;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItemStatus;
import org.example.goldenheartrestaurant.modules.order.entity.OrderStatus;
import org.example.goldenheartrestaurant.modules.order.repository.OrderItemRepository;
import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTableStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
/**
 * Quy tắc trừ kho (Stock Deduction Contract):
 * ─────────────────────────────────────────────
 * Nguyên liệu CHỈ bị trừ MỘT LẦN duy nhất khi bếp bắt đầu chế biến:
 *   PENDING (hoặc WAITING_STOCK) ──► PROCESSING  →  trừ kho ngay
 *
 * Khi bếp hoàn tất:
 *   PROCESSING ──► COMPLETED  →  chỉ đổi trạng thái, KHÔNG trừ kho thêm
 *
 * Lý do: nguyên liệu đã được dùng ngay khi bắt tay vào nấu (bước PROCESSING),
 * không phải sau khi nấu xong. Nhờ vậy:
 *  • Tránh double-deduction khi dùng cả 2 endpoint /status + /complete.
 *  • Nếu CANCEL sau khi đã PROCESSING → cần tạo stock adjustment riêng.
 */
public class KitchenWorkflowService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_KITCHEN = "KITCHEN";

    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<KitchenPendingOrderItemResponse> getPendingItems(Integer branchId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_KITCHEN);

        Integer scopedBranchId = resolveKitchenBranchScope(branchId, currentUser);
        return orderItemRepository.findKitchenItemsByStatuses(
                        EnumSet.of(OrderItemStatus.PENDING, OrderItemStatus.WAITING_STOCK, OrderItemStatus.PROCESSING),
                        scopedBranchId
                ).stream()
                .map(this::toKitchenPendingResponse)
                .toList();
    }

    /**
     * Hoàn tất một món bếp đã chế biến xong (PROCESSING → COMPLETED).
     *
     * Bắt buộc item phải đang ở PROCESSING trước khi gọi endpoint này.
     * Sử dụng changeOrderItemStatus nội bộ để tái dùng logic validate + audit.
     * KHÔNG trừ kho ở đây – kho đã được trừ khi chuyển sang PROCESSING.
     */
    @Transactional
    public OrderItemCompletionResponse completeOrderItem(Integer orderItemId, CustomUserDetails currentUser) {
        // Load trước để đưa ra lỗi có ngữ cảnh nghiệp vụ rõ ràng
        OrderItem orderItem = orderItemRepository.findKitchenDetailById(orderItemId)
                .orElseThrow(() -> new NotFoundException("Order item not found"));

        if (orderItem.getStatus() != OrderItemStatus.PROCESSING) {
            String currentStatus = orderItem.getStatus().name();
            throw new ConflictException(
                    "Món \"" + orderItem.getMenuItem().getName() + "\" đang ở trạng thái "
                    + currentStatus + ". Bếp chỉ có thể hoàn tất món đang trong quá trình chế biến (PROCESSING). "
                    + "Vui lòng chuyển sang PROCESSING trước bằng API /kitchen/order-items/{id}/status."
            );
        }

        OrderItemStatusChangeResponse result = changeOrderItemStatus(orderItemId, OrderItemStatus.COMPLETED, null, currentUser);
        return new OrderItemCompletionResponse(
                result.orderItemId(),
                result.orderId(),
                result.menuItemName(),
                result.status(),
                result.deductions()
        );
    }

    @Transactional
    public OrderItemStatusChangeResponse changeOrderItemStatus(Integer orderItemId,
                                                               OrderItemStatus targetStatus,
                                                               String reason,
                                                               CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_KITCHEN);

        OrderItem orderItem = orderItemRepository.findKitchenDetailById(orderItemId)
                .orElseThrow(() -> new NotFoundException("Order item not found"));
        enforceBranchScope(orderItem, currentUser);

        OrderItemStatus previousStatus = orderItem.getStatus();
        validateTransition(previousStatus, targetStatus);

        List<StockDeductionResponse> deductions = List.of();
        if (targetStatus == OrderItemStatus.PROCESSING) {
            return startProcessing(orderItem, previousStatus, currentUser);
        }
        if (targetStatus == OrderItemStatus.CANCELLED) {
            orderItem.setNote(buildCancelledNote(orderItem.getNote(), reason));
        }
        if (targetStatus == OrderItemStatus.COMPLETED) {
            orderItem.setNote(clearTaggedNote(orderItem.getNote(), "Stock issue"));
        }

        orderItem.setStatus(targetStatus);
        recalculateOrderStatus(orderItem.getOrder());
        orderItemRepository.save(orderItem);

        return new OrderItemStatusChangeResponse(
                orderItem.getId(),
                orderItem.getOrder().getId(),
                orderItem.getMenuItem().getName(),
                previousStatus.name(),
                targetStatus.name(),
                deductions
        );
    }

    /**
     * Kiểm tra chuyển trạng thái hợp lệ theo luồng nghiệp vụ bếp:
     *
     * PENDING ──────────────►  PROCESSING  (bếp bắt đầu nấu, trừ kho)
     * PENDING ──────────────►  CANCELLED   (huỷ trước khi nấu)
     * WAITING_STOCK ────────►  PROCESSING  (kho đã được nhập đủ, bếp tiếp tục)
     * WAITING_STOCK ────────►  CANCELLED   (huỷ vì thiếu hàng lâu)
     * PROCESSING ───────────►  COMPLETED   (nấu xong, sẵn sàng bưng)
     * PROCESSING ───────────►  CANCELLED   (huỷ giữa chừng, cần điều chỉnh kho riêng)
     *
     * Các chuyển trạng thái khác đều bị từ chối.
     */
    private void validateTransition(OrderItemStatus currentStatus, OrderItemStatus targetStatus) {
        if (currentStatus == targetStatus) {
            throw new ConflictException(
                    "Món đã ở trạng thái " + targetStatus.name() + ", không cần thay đổi.");
        }
        if (currentStatus == OrderItemStatus.CANCELLED) {
            throw new ConflictException("Không thể thay đổi trạng thái món đã bị huỷ.");
        }
        if (currentStatus == OrderItemStatus.COMPLETED || currentStatus == OrderItemStatus.SERVED) {
            throw new ConflictException(
                    "Món đã ở trạng thái " + currentStatus.name() + ", không thể thay đổi trạng thái bếp.");
        }
        if (targetStatus == OrderItemStatus.PENDING) {
            throw new ConflictException("Không thể chuyển món về trạng thái PENDING sau khi đã xử lý.");
        }
        if (targetStatus == OrderItemStatus.WAITING_STOCK) {
            throw new ConflictException("Trạng thái WAITING_STOCK được quản lý tự động khi kho không đủ hàng.");
        }
        if (targetStatus == OrderItemStatus.SERVED) {
            throw new ConflictException("Trạng thái SERVED do nhân viên phục vụ xác nhận qua API riêng, không phải bếp.");
        }

        // PENDING / WAITING_STOCK → PROCESSING hoặc CANCELLED: hợp lệ
        if ((currentStatus == OrderItemStatus.PENDING || currentStatus == OrderItemStatus.WAITING_STOCK)
                && (targetStatus == OrderItemStatus.PROCESSING || targetStatus == OrderItemStatus.CANCELLED)) {
            return;
        }
        // PROCESSING → COMPLETED hoặc CANCELLED: hợp lệ
        if (currentStatus == OrderItemStatus.PROCESSING
                && (targetStatus == OrderItemStatus.COMPLETED || targetStatus == OrderItemStatus.CANCELLED)) {
            return;
        }

        throw new ConflictException(
                "Không hỗ trợ chuyển trạng thái từ " + currentStatus.name() + " sang " + targetStatus.name() + ". "
                + "Luồng bếp: PENDING → PROCESSING → COMPLETED.");
    }

    private OrderItemStatusChangeResponse startProcessing(OrderItem orderItem,
                                                          OrderItemStatus previousStatus,
                                                          CustomUserDetails currentUser) {
        try {
            List<StockDeductionResponse> deductions = deductStockForProcessing(orderItem, currentUser);
            orderItem.setNote(clearTaggedNote(orderItem.getNote(), "Stock issue"));
            orderItem.setStatus(OrderItemStatus.PROCESSING);
            recalculateOrderStatus(orderItem.getOrder());
            orderItemRepository.save(orderItem);

            return new OrderItemStatusChangeResponse(
                    orderItem.getId(),
                    orderItem.getOrder().getId(),
                    orderItem.getMenuItem().getName(),
                    previousStatus.name(),
                    OrderItemStatus.PROCESSING.name(),
                    deductions
            );
        } catch (ConflictException exception) {
            // Chỉ chuyển sang WAITING_STOCK khi đây thật sự là lỗi thiếu hàng.
            // Các lỗi cấu hình như "chưa có recipe" hoặc "không có inventory record"
            // cần nổi lên rõ ràng để đội vận hành/sản phẩm sửa dữ liệu gốc,
            // thay vì bị ngụy trang thành "hết hàng".
            if (!isRecoverableStockIssue(exception.getMessage())) {
                throw exception;
            }

            orderItem.setStatus(OrderItemStatus.WAITING_STOCK);
            orderItem.setNote(buildStockIssueNote(orderItem.getNote(), exception.getMessage()));
            recalculateOrderStatus(orderItem.getOrder());
            orderItemRepository.save(orderItem);

            return new OrderItemStatusChangeResponse(
                    orderItem.getId(),
                    orderItem.getOrder().getId(),
                    orderItem.getMenuItem().getName(),
                    previousStatus.name(),
                    OrderItemStatus.WAITING_STOCK.name(),
                    List.of()
            );
        }
    }

    private boolean isRecoverableStockIssue(String message) {
        return StringUtils.hasText(message)
                && message.startsWith("Insufficient stock");
    }

    private String buildCancelledNote(String existingNote, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new ConflictException("Kitchen cancellation reason is required");
        }

        String cancellationNote = "Kitchen cancel reason: " + reason.trim();
        String mergedNote = StringUtils.hasText(existingNote)
                ? existingNote.trim() + " | " + cancellationNote
                : cancellationNote;

        if (mergedNote.length() > 255) {
            throw new ConflictException("Cancellation reason is too long to fit into the order note");
        }
        return mergedNote;
    }

    private String buildStockIssueNote(String existingNote, String shortageMessage) {
        if (!StringUtils.hasText(shortageMessage)) {
            return existingNote;
        }

        return appendTaggedNote(existingNote, "Stock issue", shortageMessage.trim());
    }

    private List<StockDeductionResponse> deductStockForProcessing(OrderItem orderItem, CustomUserDetails currentUser) {
        List<Recipe> recipes = orderItem.getMenuItem().getRecipes();
        if (recipes.isEmpty()) {
            throw new ConflictException("Menu item has no recipe configured");
        }

        List<Integer> ingredientIds = recipes.stream()
                .map(recipe -> recipe.getIngredient().getId())
                .toList();

        Map<Integer, Inventory> inventoryMap = inventoryRepository
                .findAllForUpdateByBranchIdAndIngredientIds(orderItem.getOrder().getBranch().getId(), ingredientIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(inv -> inv.getIngredient().getId(), Function.identity()));

        User actor = userRepository.findEmployeeDetailById(currentUser.getUserId())
                .orElseThrow(() -> new NotFoundException("Current user not found"));

        List<StockDeductionResponse> deductions = new ArrayList<>();
        List<StockMovement> stockMovements = new ArrayList<>();

        for (Recipe recipe : recipes) {
            Inventory inventory = inventoryMap.get(recipe.getIngredient().getId());
            if (inventory == null) {
                throw new ConflictException("Inventory not found for ingredient: " + recipe.getIngredient().getName());
            }

            BigDecimal quantityToDeduct = recipe.getQuantity().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
            BigDecimal currentStock = inventory.getQuantity() == null ? BigDecimal.ZERO : inventory.getQuantity();

            if (currentStock.compareTo(quantityToDeduct) < 0) {
                throw new ConflictException("Insufficient stock for ingredient: "
                        + recipe.getIngredient().getName()
                        + " (need "
                        + quantityToDeduct.stripTrailingZeros().toPlainString()
                        + ", available "
                        + currentStock.stripTrailingZeros().toPlainString()
                        + ")");
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
                            .totalCost(unitCost.multiply(quantityToDeduct))
                            .occurredAt(LocalDateTime.now())
                            .note("Stock deducted after kitchen started processing order item")
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
        return deductions;
    }

    private String appendTaggedNote(String existingNote, String tag, String message) {
        String cleanedNote = clearTaggedNote(existingNote, tag);
        String taggedMessage = tag + ": " + message;
        String mergedNote = StringUtils.hasText(cleanedNote)
                ? cleanedNote + " | " + taggedMessage
                : taggedMessage;

        if (mergedNote.length() > 255) {
            throw new ConflictException(tag + " is too long to fit into the order note");
        }
        return mergedNote;
    }

    private String clearTaggedNote(String existingNote, String tag) {
        if (!StringUtils.hasText(existingNote)) {
            return null;
        }

        List<String> keptSegments = new ArrayList<>();
        for (String segment : existingNote.split("\\s*\\|\\s*")) {
            String normalizedSegment = segment.trim();
            if (!normalizedSegment.startsWith(tag + ":")) {
                keptSegments.add(normalizedSegment);
            }
        }

        return keptSegments.isEmpty() ? null : String.join(" | ", keptSegments);
    }

    private void recalculateOrderStatus(Order order) {
        boolean allCancelled = order.getOrderItems().stream()
                .allMatch(item -> item.getStatus() == OrderItemStatus.CANCELLED);
        boolean allFinal = order.getOrderItems().stream()
                .allMatch(item -> item.getStatus() == OrderItemStatus.COMPLETED
                        || item.getStatus() == OrderItemStatus.SERVED
                        || item.getStatus() == OrderItemStatus.CANCELLED);
        boolean anyInProgress = order.getOrderItems().stream()
                .anyMatch(item -> item.getStatus() == OrderItemStatus.PROCESSING
                        || item.getStatus() == OrderItemStatus.COMPLETED
                        || item.getStatus() == OrderItemStatus.SERVED);

        if (allCancelled) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setClosedAt(LocalDateTime.now());
            if (order.getTable() != null) {
                order.getTable().setStatus(RestaurantTableStatus.AVAILABLE);
            }
            return;
        }

        if (allFinal) {
            order.setStatus(OrderStatus.COMPLETED);
            return;
        }

        order.setClosedAt(null);
        order.setStatus(anyInProgress ? OrderStatus.PROCESSING : OrderStatus.PENDING);
    }

    private Integer resolveKitchenBranchScope(Integer branchId, CustomUserDetails currentUser) {
        if (hasRole(currentUser, ROLE_ADMIN) || hasRole(currentUser, ROLE_MANAGER)) {
            return branchId;
        }

        User currentUserEntity = userRepository.findEmployeeDetailById(currentUser.getUserId())
                .orElseThrow(() -> new NotFoundException("Current user not found"));
        if (currentUserEntity.getProfile() == null || currentUserEntity.getProfile().getBranch() == null) {
            throw new ForbiddenException("Your account is not assigned to any branch");
        }

        Integer ownBranchId = currentUserEntity.getProfile().getBranch().getId();
        if (branchId != null && !branchId.equals(ownBranchId)) {
            throw new ForbiddenException("You do not have permission to view another branch kitchen");
        }
        return ownBranchId;
    }

    private void enforceBranchScope(OrderItem orderItem, CustomUserDetails currentUser) {
        if (hasRole(currentUser, ROLE_ADMIN)) {
            return;
        }

        User currentUserEntity = userRepository.findEmployeeDetailById(currentUser.getUserId())
                .orElseThrow(() -> new NotFoundException("Current user not found"));
        if (currentUserEntity.getProfile() == null || currentUserEntity.getProfile().getBranch() == null) {
            throw new ForbiddenException("Your account is not assigned to any branch");
        }
        Integer ownBranchId = currentUserEntity.getProfile().getBranch().getId();
        if (!ownBranchId.equals(orderItem.getOrder().getBranch().getId())) {
            throw new ForbiddenException("You do not have permission to change another branch kitchen item");
        }
    }

    private KitchenPendingOrderItemResponse toKitchenPendingResponse(OrderItem orderItem) {
        return new KitchenPendingOrderItemResponse(
                orderItem.getId(),
                orderItem.getOrder().getId(),
                orderItem.getOrder().getTable() != null ? orderItem.getOrder().getTable().getId() : null,
                orderItem.getOrder().getTable() != null ? orderItem.getOrder().getTable().getTableNumber() : null,
                orderItem.getMenuItem().getId(),
                orderItem.getMenuItem().getName(),
                orderItem.getQuantity(),
                orderItem.getNote(),
                orderItem.getOrder().getCreatedAt(),
                orderItem.getStatus().name()
        );
    }

    private boolean hasRole(CustomUserDetails currentUser, String role) {
        return role.equalsIgnoreCase(currentUser.getRoleName());
    }

    private void requireAnyRole(CustomUserDetails currentUser, String... roles) {
        for (String role : roles) {
            if (hasRole(currentUser, role)) {
                return;
            }
        }
        throw new ForbiddenException("You do not have permission to perform this action");
    }
}
