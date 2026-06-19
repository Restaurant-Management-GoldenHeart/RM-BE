package org.example.goldenheartrestaurant.modules.order.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.modules.billing.entity.Bill;
import org.example.goldenheartrestaurant.modules.billing.repository.BillRepository;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.customer.entity.Customer;
import org.example.goldenheartrestaurant.modules.customer.repository.CustomerRepository;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.example.goldenheartrestaurant.modules.inventory.entity.Inventory;
import org.example.goldenheartrestaurant.modules.inventory.repository.InventoryRepository;
import org.example.goldenheartrestaurant.modules.menu.entity.MenuItem;
import org.example.goldenheartrestaurant.modules.menu.entity.MenuItemStatus;
import org.example.goldenheartrestaurant.modules.menu.repository.MenuItemRepository;
import org.example.goldenheartrestaurant.modules.order.dto.request.CreateOrderRequest;
import org.example.goldenheartrestaurant.modules.order.dto.request.OrderItemRequest;
import org.example.goldenheartrestaurant.modules.order.dto.response.OrderItemResponse;
import org.example.goldenheartrestaurant.modules.order.dto.response.OrderItemSummaryResponse;
import org.example.goldenheartrestaurant.modules.order.dto.response.OrderItemStatusChangeResponse;
import org.example.goldenheartrestaurant.modules.order.dto.response.OrderResponse;
import org.example.goldenheartrestaurant.modules.order.entity.Order;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItem;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItemStatus;
import org.example.goldenheartrestaurant.modules.order.entity.OrderStatus;
import org.example.goldenheartrestaurant.modules.order.repository.OrderItemRepository;
import org.example.goldenheartrestaurant.modules.order.repository.OrderRepository;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTable;
import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTableStatus;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.MergeTablesRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.SplitOrderItemRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.SplitTableRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.UnmergeTableTargetRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.UnmergeTablesRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.response.TableOrderTransferResponse;
import org.example.goldenheartrestaurant.modules.restaurant.dto.response.UnmergeTableResultResponse;
import org.example.goldenheartrestaurant.modules.restaurant.dto.response.UnmergeTablesResponse;
import org.example.goldenheartrestaurant.modules.restaurant.repository.BranchRepository;
import org.example.goldenheartrestaurant.modules.restaurant.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Service trung tâm xử lý vòng đời đơn hàng tại quầy.
 *
 * Các trách nhiệm chính:
 * - mở order mới hoặc nối thêm món vào order đang hoạt động
 * - tách bàn
 * - gộp bàn
 * - gán khách vào order
 * - đánh dấu món đã phục vụ
 * - đồng bộ trạng thái order và trạng thái bàn
 *
 * Điểm khó của lớp này là khái niệm "bàn thao tác" và "bàn vận hành thực sự" có thể khác nhau.
 * Khi bàn đã thuộc một nhóm gộp, request có thể đi vào bàn thành viên nhưng hệ thống phải tự quy
 * về bàn gốc để tránh tạo nhiều active order cho cùng một nhóm bàn.
 */
@Service
@RequiredArgsConstructor
public class OrderManagementService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_STAFF = "STAFF";
    private static final String ROLE_KITCHEN = "KITCHEN";
    private static final Comparator<RestaurantTable> TABLE_GROUP_COMPARATOR = Comparator
            .comparing(RestaurantTable::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(RestaurantTable::getTableNumber, String.CASE_INSENSITIVE_ORDER);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BillRepository billRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final BranchRepository branchRepository;
    private final MenuItemRepository menuItemRepository;
    private final InventoryRepository inventoryRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF);

        User actor = getCurrentUser(currentUser.getUserId());
        RestaurantTable requestedTable = resolveTable(request.tableId());
        // Nếu request chỉ vào một bàn thành viên của nhóm gộp,
        // hệ thống phải quy về bàn gốc để toàn bộ món vẫn đi vào cùng một active order.
        RestaurantTable operationalTable = resolveOperationalTable(requestedTable);
        Integer branchId = resolveBranchId(request.branchId(), operationalTable);
        enforceBranchScope(actor, currentUser, branchId);

        Branch branch = resolveBranch(branchId);
        Customer customer = resolveCustomer(request.customerId());

        // Có bàn + đã có active order  -> gọi thêm món vào order cũ.
        // Có bàn + chưa có active order -> mở order mới cho bàn đó.
        // Không có bàn                   -> order mang tính quầy/lấy đi.
        Order order = operationalTable != null
                ? findActiveOrderEntityByTableId(operationalTable.getId()).orElse(null)
                : null;

        if (order == null) {
            validateTableCanOpenOrder(operationalTable);
            order = Order.builder()
                    .branch(branch)
                    .table(operationalTable)
                    .customer(customer)
                    .createdBy(actor)
                    .status(OrderStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .closedAt(null)
                    .build();
        } else {
            if (!order.getBranch().getId().equals(branch.getId())) {
                throw new ConflictException("Active order branch does not match requested branch");
            }
            if (customer != null) {
                order.setCustomer(customer);
            }
        }

        appendItems(order, request.items(), branch);
        recalculateOrderStatus(order);

        Order savedOrder = orderRepository.save(order);

        if (operationalTable != null) {
            // Khi nhóm bàn có active order, toàn bộ nhóm phải cùng ở trạng thái OCCUPIED.
            List<RestaurantTable> mergedGroup = loadMergedGroup(operationalTable);
            for (RestaurantTable groupTable : mergedGroup) {
                groupTable.setStatus(RestaurantTableStatus.OCCUPIED);
            }
            restaurantTableRepository.saveAll(mergedGroup);
        }

        return toOrderResponse(loadOrder(savedOrder.getId()));
    }

    @Transactional
    public OrderItemStatusChangeResponse serveOrderItem(Integer orderItemId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF);

        OrderItem orderItem = orderItemRepository.findKitchenDetailById(orderItemId)
                .orElseThrow(() -> new NotFoundException("Order item not found"));
        enforceReadScope(orderItem.getOrder(), currentUser);

        if (orderItem.getStatus() == OrderItemStatus.SERVED) {
            throw new ConflictException("Order item is already served");
        }
        if (orderItem.getStatus() != OrderItemStatus.COMPLETED) {
            throw new ConflictException("Only completed kitchen items can be marked as served");
        }

        orderItem.setStatus(OrderItemStatus.SERVED);
        recalculateOrderStatus(orderItem.getOrder());
        orderItemRepository.save(orderItem);

        return new OrderItemStatusChangeResponse(
                orderItem.getId(),
                orderItem.getOrder().getId(),
                orderItem.getMenuItem().getName(),
                OrderItemStatus.COMPLETED.name(),
                OrderItemStatus.SERVED.name(),
                List.of()
        );
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Integer orderId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF, ROLE_KITCHEN);

        Order order = loadOrder(orderId);
        enforceReadScope(order, currentUser);
        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse assignCustomerToOrder(Integer orderId, Integer customerId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF);

        Order order = loadOrder(orderId);
        enforceReadScope(order, currentUser);
        ensureOrderCustomerCanBeUpdated(order);

        order.setCustomer(resolveCustomer(customerId));
        return toOrderResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getActiveOrderByTableId(Integer tableId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF, ROLE_KITCHEN);

        RestaurantTable table = resolveTable(tableId);
        enforceTableReadScope(table, currentUser);

        return findActiveOrderEntityByTableId(tableId)
                .map(this::toOrderResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Order findOrderEntityById(Integer orderId) {
        return loadOrder(orderId);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Order> findActiveOrderEntityByTableId(Integer tableId) {
        if (tableId == null) {
            return java.util.Optional.empty();
        }

        RestaurantTable table = resolveTable(tableId);
        RestaurantTable operationalTable = resolveOperationalTable(table);

        return orderRepository.findActiveOrdersByTableId(operationalTable.getId())
                .stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .findFirst();
    }

    @Transactional
    public TableOrderTransferResponse splitTable(Integer sourceTableId,
                                                 SplitTableRequest request,
                                                 CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF);

        RestaurantTable sourceTable = resolveTable(sourceTableId);
        RestaurantTable targetTable = resolveTable(request.targetTableId());
        validateTableTransferScope(sourceTable, targetTable, currentUser);
        // Chỉ cho tách giữa hai bàn độc lập.
        // Nếu bàn đã nằm trong nhóm gộp thì việc xác định "nguồn" và "đích" sẽ rất mơ hồ.
        ensureStandaloneTable(sourceTable, "Source table");
        ensureStandaloneTable(targetTable, "Target table");

        Order sourceOrder = findActiveOrderEntityByTableId(sourceTableId)
                .orElseThrow(() -> new ConflictException("Source table does not have an active order"));
        ensureOrderHasNoBilling(sourceOrder);

        if (findActiveOrderEntityByTableId(targetTable.getId()).isPresent()) {
            throw new ConflictException("Target table already has an active order");
        }
        if (targetTable.getStatus() != RestaurantTableStatus.AVAILABLE
                && targetTable.getStatus() != RestaurantTableStatus.RESERVED) {
            throw new ConflictException("Target table is not ready to receive a split order");
        }

        int sourceQuantity = countBillableQuantity(sourceOrder);
        if (sourceQuantity < 2) {
            throw new ConflictException("A table can only be split when the source order has at least two dishes");
        }

        User actor = getCurrentUser(currentUser.getUserId());
        Order targetOrder = Order.builder()
                .branch(sourceOrder.getBranch())
                .table(targetTable)
                .customer(sourceOrder.getCustomer())
                .createdBy(actor)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .closedAt(null)
                .build();

        // Thao tác tách có thể:
        // - chuyển nguyên line item sang order mới
        // - hoặc tách bớt quantity khỏi line cũ để tạo line mới ở order đích
        int movedQuantity = moveSelectedItemsToNewOrder(sourceOrder, targetOrder, request.items());
        if (movedQuantity <= 0) {
            throw new ConflictException("At least one dish must be selected for splitting");
        }
        if (countBillableQuantity(sourceOrder) == 0) {
            throw new ConflictException("Source table must still retain at least one dish after split");
        }

        recalculateOrderStatus(sourceOrder);
        recalculateOrderStatus(targetOrder);
        targetTable.setStatus(RestaurantTableStatus.OCCUPIED);

        orderRepository.save(sourceOrder);
        Order savedTargetOrder = orderRepository.save(targetOrder);
        restaurantTableRepository.save(targetTable);

        return buildTableTransferResponse("SPLIT", sourceTable, sourceOrder, targetTable, loadOrder(savedTargetOrder.getId()));
    }

    @Transactional
    public TableOrderTransferResponse mergeTables(MergeTablesRequest request, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF);

        if (request.sourceTableId().equals(request.targetTableId())) {
            throw new ConflictException("Source table and target table must be different");
        }

        RestaurantTable requestedSourceTable = resolveTable(request.sourceTableId());
        RestaurantTable requestedTargetTable = resolveTable(request.targetTableId());
        validateTableTransferScope(requestedSourceTable, requestedTargetTable, currentUser);

        // API có thể nhận vào bàn gốc hoặc bàn thành viên.
        // Trước khi gộp, luôn quy hai đầu về bàn gốc hiện tại của mỗi nhóm.
        RestaurantTable sourceTable = resolveOperationalTable(requestedSourceTable);
        RestaurantTable targetTable = resolveOperationalTable(requestedTargetTable);
        if (sourceTable.getId().equals(targetTable.getId())) {
            throw new ConflictException("Selected tables already belong to the same merged-table group");
        }

        Order sourceOrder = findActiveOrderEntityByTableId(sourceTable.getId())
                .orElseThrow(() -> new ConflictException("Source table does not have an active order"));
        Order targetOrder = findActiveOrderEntityByTableId(targetTable.getId())
                .orElseThrow(() -> new ConflictException("Target table does not have an active order"));

        ensureOrderHasNoBilling(sourceOrder);
        ensureOrderHasNoBilling(targetOrder);

        if (countBillableQuantity(sourceOrder) < 1 || countBillableQuantity(targetOrder) < 1) {
            throw new ConflictException("Both tables must each have at least one dish before merging");
        }

        // Gộp bàn thực chất là nhập toàn bộ line item từ source order sang target order.
        // Source order sau đó bị đóng ở trạng thái CANCELLED để giữ lịch sử nghiệp vụ.
        List<OrderItem> itemsToMove = List.copyOf(sourceOrder.getOrderItems());
        for (OrderItem item : itemsToMove) {
            targetOrder.getOrderItems().add(item);
            item.setOrder(targetOrder);
            sourceOrder.getOrderItems().remove(item);
        }

        // Nếu order đích chưa có khách thì ưu tiên kế thừa khách của order nguồn.
        if (targetOrder.getCustomer() == null) {
            targetOrder.setCustomer(sourceOrder.getCustomer());
        }

        List<RestaurantTable> sourceGroup = loadMergedGroup(sourceTable);
        List<RestaurantTable> targetGroup = loadMergedGroup(targetTable);
        for (RestaurantTable sourceGroupTable : sourceGroup) {
            if (!sourceGroupTable.getId().equals(targetTable.getId())) {
                // Mọi bàn ở nhóm nguồn sẽ trở thành thành viên của nhóm đích.
                sourceGroupTable.setMergedIntoTable(targetTable);
            }
            sourceGroupTable.setStatus(RestaurantTableStatus.OCCUPIED);
        }
        for (RestaurantTable targetGroupTable : targetGroup) {
            targetGroupTable.setStatus(RestaurantTableStatus.OCCUPIED);
        }

        // Sau khi toàn bộ món đã nhập vào order đích, order nguồn không còn là active order nữa.
        sourceOrder.setStatus(OrderStatus.CANCELLED);
        sourceOrder.setClosedAt(LocalDateTime.now());
        targetTable.setStatus(RestaurantTableStatus.OCCUPIED);

        recalculateOrderStatus(targetOrder);
        orderRepository.save(sourceOrder);
        Order savedTargetOrder = orderRepository.save(targetOrder);
        restaurantTableRepository.saveAll(mergeDistinctTables(sourceGroup, targetGroup));

        return buildTableTransferResponse("MERGE", sourceTable, sourceOrder, targetTable, loadOrder(savedTargetOrder.getId()));
    }

    @Transactional
    public UnmergeTablesResponse unmergeTables(Integer rootTableId,
                                               UnmergeTablesRequest request,
                                               CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF);

        RestaurantTable requestedRootTable = resolveTable(rootTableId);
        RestaurantTable rootTable = resolveOperationalTable(requestedRootTable);
        enforceTableReadScope(rootTable, currentUser);

        List<RestaurantTable> mergedGroup = loadMergedGroup(rootTable);
        if (mergedGroup.size() < 2) {
            throw new ConflictException("Selected table is not a merged-table root");
        }

        Order rootOrder = findActiveOrderEntityByTableId(rootTable.getId())
                .orElseThrow(() -> new ConflictException("Merged root table does not have an active order"));
        ensureOrderHasNoBilling(rootOrder);
        validateUnmergeSelections(rootTable, mergedGroup, rootOrder, request.targets(), currentUser);

        User actor = getCurrentUser(currentUser.getUserId());
        Map<Integer, Order> targetOrders = new LinkedHashMap<>();

        for (UnmergeTableTargetRequest targetRequest : request.targets()) {
            RestaurantTable targetTable = resolveTable(targetRequest.targetTableId());
            Order targetOrder = createDerivedOrder(rootOrder, targetTable, actor);
            int movedQuantity = moveSelectedItemsToNewOrder(rootOrder, targetOrder, targetRequest.items());
            if (movedQuantity <= 0) {
                throw new ConflictException("Each target table must receive at least one dish");
            }
            recalculateOrderStatus(targetOrder);
            targetTable.setStatus(RestaurantTableStatus.OCCUPIED);
            targetOrders.put(targetTable.getId(), targetOrder);
        }

        int remainingQuantity = countBillableQuantity(rootOrder);
        if (remainingQuantity > 0) {
            recalculateOrderStatus(rootOrder);
            rootTable.setStatus(RestaurantTableStatus.OCCUPIED);
        } else {
            rootOrder.setStatus(OrderStatus.CANCELLED);
            rootOrder.setClosedAt(LocalDateTime.now());
            rootTable.setStatus(RestaurantTableStatus.AVAILABLE);
        }

        for (RestaurantTable groupedTable : mergedGroup) {
            if (!groupedTable.getId().equals(rootTable.getId())) {
                groupedTable.setMergedIntoTable(null);
                if (!targetOrders.containsKey(groupedTable.getId())) {
                    groupedTable.setStatus(RestaurantTableStatus.AVAILABLE);
                }
            }
        }

        Order savedRootOrder = orderRepository.save(rootOrder);
        List<Order> savedTargetOrders = orderRepository.saveAll(targetOrders.values());
        restaurantTableRepository.saveAll(mergedGroup);

        return buildUnmergeResponse(rootTable, savedRootOrder, savedTargetOrders, mergedGroup);
    }

    private void appendItems(Order order, List<OrderItemRequest> requests, Branch branch) {
        for (OrderItemRequest request : requests) {
            MenuItem menuItem = resolveMenuItemForOrder(request.menuItemId(), branch.getId());
            String normalizedNote = normalizeNote(request.note());

            OrderItem existingItem = order.getOrderItems().stream()
                    .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
                    .filter(item -> item.getMenuItem().getId().equals(menuItem.getId()))
                    .filter(item -> Objects.equals(normalizeNote(item.getNote()), normalizedNote))
                    .findFirst()
                    .orElse(null);

            // Nếu món, ghi chú và trạng thái phù hợp thì ưu tiên cộng dồn quantity
            // thay vì tạo thêm line item mới để bill và giao diện gọn hơn.
            if (existingItem != null
                    // Chỉ gộp thêm số lượng vào line còn "chưa vào bếp thật sự".
                    // Nếu line đã PROCESSING thì stock đã bị deduct theo quantity cũ,
                    // nên cộng quantity vào cùng line sẽ làm lệch tồn kho và lệch flow bếp.
                    && (existingItem.getStatus() == OrderItemStatus.PENDING
                    || existingItem.getStatus() == OrderItemStatus.WAITING_STOCK)) {
                existingItem.setQuantity(existingItem.getQuantity() + request.quantity());
                continue;
            }

            order.getOrderItems().add(
                    OrderItem.builder()
                            .order(order)
                            .menuItem(menuItem)
                            .quantity(request.quantity())
                            .price(menuItem.getPrice())
                            .status(OrderItemStatus.PENDING)
                            .note(normalizedNote)
                            .build()
            );
        }
    }

    private Integer resolveBranchId(Integer requestedBranchId, RestaurantTable table) {
        if (table != null) {
            Integer tableBranchId = table.getBranch().getId();
            // Nếu request truyền đồng thời tableId và branchId,
            // hệ thống dùng branch của bàn làm chuẩn và kiểm tra tính nhất quán.
            if (requestedBranchId != null && !requestedBranchId.equals(tableBranchId)) {
                throw new ConflictException("Table does not belong to the requested branch");
            }
            return tableBranchId;
        }

        if (requestedBranchId == null) {
            throw new ConflictException("Either tableId or branchId is required");
        }

        return requestedBranchId;
    }

    private MenuItem resolveMenuItemForOrder(Integer menuItemId, Integer branchId) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new NotFoundException("Menu item not found"));

        if (!menuItem.getBranch().getId().equals(branchId)) {
            throw new ConflictException("Menu item does not belong to the selected branch");
        }
        if (!isMenuItemOrderable(menuItem)) {
            throw new ConflictException("Menu item is not available for ordering");
        }

        return menuItem;
    }

    private boolean isMenuItemOrderable(MenuItem menuItem) {
        if (menuItem.getStatus() != MenuItemStatus.AVAILABLE) {
            return false;
        }

        if (menuItem.getRecipes() == null || menuItem.getRecipes().isEmpty()) {
            return false;
        }

        List<Integer> ingredientIds = menuItem.getRecipes().stream()
                .map(recipe -> recipe.getIngredient().getId())
                .distinct()
                .toList();

        Map<Integer, Inventory> inventoryByIngredientId = ingredientIds.isEmpty()
                ? Map.of()
                : inventoryRepository.findAllByBranchIdAndIngredientIds(menuItem.getBranch().getId(), ingredientIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        inventory -> inventory.getIngredient().getId(),
                        java.util.function.Function.identity()
                ));

        return menuItem.getRecipes().stream().allMatch(recipe -> {
            Inventory inventory = inventoryByIngredientId.get(recipe.getIngredient().getId());
            if (inventory == null) {
                return false;
            }

            BigDecimal currentQuantity = inventory.getQuantity() != null ? inventory.getQuantity() : BigDecimal.ZERO;
            BigDecimal requiredQuantity = recipe.getQuantity() != null ? recipe.getQuantity() : BigDecimal.ZERO;
            return currentQuantity.compareTo(requiredQuantity) >= 0;
        });
    }

    private Branch resolveBranch(Integer branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found"));
    }

    private RestaurantTable resolveTable(Integer tableId) {
        if (tableId == null) {
            return null;
        }

        return restaurantTableRepository.findDetailById(tableId)
                .orElseThrow(() -> new NotFoundException("Table not found"));
    }

    private void validateTableCanOpenOrder(RestaurantTable table) {
        if (table == null) {
            return;
        }
        if (table.getStatus() == RestaurantTableStatus.CLEANING) {
            throw new ConflictException("Table is being cleaned and cannot receive a new order");
        }
        if (table.getStatus() == RestaurantTableStatus.OCCUPIED) {
            throw new ConflictException("Table is already occupied");
        }
    }

    private Customer resolveCustomer(Integer customerId) {
        if (customerId == null) {
            return null;
        }

        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    private User getCurrentUser(Integer userId) {
        return userRepository.findEmployeeDetailById(userId)
                .orElseThrow(() -> new NotFoundException("Current user not found"));
    }

    private void enforceReadScope(Order order, CustomUserDetails currentUser) {
        if (hasRole(currentUser, ROLE_ADMIN) || hasRole(currentUser, ROLE_MANAGER)) {
            return;
        }

        Integer scopedBranchId = extractBranchId(getCurrentUser(currentUser.getUserId()));
        if (scopedBranchId == null || !scopedBranchId.equals(order.getBranch().getId())) {
            throw new ForbiddenException("You do not have permission to access this order");
        }
    }

    private void enforceTableReadScope(RestaurantTable table, CustomUserDetails currentUser) {
        if (hasRole(currentUser, ROLE_ADMIN) || hasRole(currentUser, ROLE_MANAGER)) {
            return;
        }

        Integer scopedBranchId = extractBranchId(getCurrentUser(currentUser.getUserId()));
        if (scopedBranchId == null || !scopedBranchId.equals(table.getBranch().getId())) {
            throw new ForbiddenException("You do not have permission to access this table");
        }
    }

    private void enforceBranchScope(User actor, CustomUserDetails currentUser, Integer branchId) {
        if (hasRole(currentUser, ROLE_ADMIN) || hasRole(currentUser, ROLE_MANAGER)) {
            return;
        }

        Integer scopedBranchId = extractBranchId(actor);
        if (scopedBranchId == null) {
            throw new ForbiddenException("Your account is not assigned to any branch");
        }
        if (!scopedBranchId.equals(branchId)) {
            throw new ForbiddenException("You do not have permission to create orders for another branch");
        }
    }

    private void validateTableTransferScope(RestaurantTable sourceTable,
                                            RestaurantTable targetTable,
                                            CustomUserDetails currentUser) {
        if (!sourceTable.getBranch().getId().equals(targetTable.getBranch().getId())) {
            throw new ConflictException("Tables must belong to the same branch");
        }
        enforceTableReadScope(sourceTable, currentUser);
        enforceTableReadScope(targetTable, currentUser);
    }

    private Integer extractBranchId(User user) {
        if (user.getProfile() == null || user.getProfile().getBranch() == null) {
            return null;
        }
        return user.getProfile().getBranch().getId();
    }

    private Order loadOrder(Integer orderId) {
        return orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private void recalculateOrderStatus(Order order) {
        // Đây là hàm đồng bộ trạng thái order theo trạng thái thực của các món.
        // Mọi luồng như create, split, merge, kitchen update, serve... đều nên đi qua đây
        // để tránh mỗi nơi tự set status theo một kiểu khác nhau.
        List<OrderItem> items = order.getOrderItems();
        boolean allCancelled = items.stream().allMatch(item -> item.getStatus() == OrderItemStatus.CANCELLED);
        boolean allFinal = items.stream().allMatch(item ->
                item.getStatus() == OrderItemStatus.COMPLETED
                        || item.getStatus() == OrderItemStatus.SERVED
                        || item.getStatus() == OrderItemStatus.CANCELLED
        );
        boolean anyInProgress = items.stream().anyMatch(item ->
                item.getStatus() == OrderItemStatus.PROCESSING
                        || item.getStatus() == OrderItemStatus.COMPLETED
                        || item.getStatus() == OrderItemStatus.SERVED
        );

        if (items.isEmpty() || allCancelled) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setClosedAt(LocalDateTime.now());
            if (order.getTable() != null) {
                // Nếu order gắn với một nhóm bàn gộp, việc đóng order phải giải phóng cả nhóm.
                releaseMergedGroup(order.getTable(), RestaurantTableStatus.AVAILABLE, true);
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

    private int moveSelectedItemsToNewOrder(Order sourceOrder,
                                            Order targetOrder,
                                            List<SplitOrderItemRequest> selections) {
        int movedQuantity = 0;

        for (SplitOrderItemRequest selection : selections) {
            OrderItem sourceItem = sourceOrder.getOrderItems().stream()
                    .filter(item -> item.getId().equals(selection.orderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ConflictException("Selected order item does not belong to the source table"));

            if (sourceItem.getStatus() == OrderItemStatus.CANCELLED) {
                throw new ConflictException("Cancelled dishes cannot be split to another table");
            }
            if (selection.quantity() > sourceItem.getQuantity()) {
                throw new ConflictException("Split quantity exceeds the source dish quantity");
            }

            movedQuantity += selection.quantity();

            if (selection.quantity().equals(sourceItem.getQuantity())) {
                targetOrder.getOrderItems().add(sourceItem);
                sourceItem.setOrder(targetOrder);
                sourceOrder.getOrderItems().remove(sourceItem);
                continue;
            }

            sourceItem.setQuantity(sourceItem.getQuantity() - selection.quantity());
            targetOrder.getOrderItems().add(
                    OrderItem.builder()
                            .order(targetOrder)
                            .menuItem(sourceItem.getMenuItem())
                            .quantity(selection.quantity())
                            .price(sourceItem.getPrice())
                            .status(sourceItem.getStatus())
                            .note(sourceItem.getNote())
                            .build()
            );
        }

        return movedQuantity;
    }

    private Order createDerivedOrder(Order sourceOrder, RestaurantTable targetTable, User actor) {
        ensureNoDirectActiveOrder(targetTable);

        return Order.builder()
                .branch(sourceOrder.getBranch())
                .table(targetTable)
                .customer(sourceOrder.getCustomer())
                .createdBy(actor)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .closedAt(null)
                .build();
    }

    private int countBillableQuantity(Order order) {
        return order.getOrderItems().stream()
                .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    private void ensureOrderHasNoBilling(Order order) {
        if (billRepository.existsByOrder_Id(order.getId())) {
            throw new ConflictException("Orders that already have bill records cannot be split or merged");
        }
    }

    private void ensureNoDirectActiveOrder(RestaurantTable table) {
        boolean hasDirectActiveOrder = orderRepository.findActiveOrdersByTableId(table.getId())
                .stream()
                .findFirst()
                .isPresent();
        if (hasDirectActiveOrder) {
            throw new ConflictException("Target table already has an active order");
        }
    }

    private void ensureOrderCustomerCanBeUpdated(Order order) {
        if (order.getClosedAt() != null || order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Closed orders cannot update customer information");
        }

        boolean hasRecordedPayments = billRepository.findAllDetailsByOrderId(order.getId())
                .stream()
                .anyMatch(bill -> !bill.getPayments().isEmpty());
        if (hasRecordedPayments) {
            throw new ConflictException("Customer information cannot be changed after payments are recorded");
        }
    }

    private TableOrderTransferResponse buildTableTransferResponse(String action,
                                                                 RestaurantTable sourceTable,
                                                                 Order sourceOrder,
                                                                 RestaurantTable targetTable,
                                                                 Order targetOrder) {
        // Response này cố ý trả cả:
        // - tên bàn gốc
        // - tên hiển thị của nhóm bàn sau thao tác
        // để phía client/Postman quan sát được rõ kết quả split/merge.
        List<RestaurantTable> sourceGroup = loadMergedGroup(resolveOperationalTable(sourceTable));
        List<RestaurantTable> targetGroup = loadMergedGroup(resolveOperationalTable(targetTable));

        return new TableOrderTransferResponse(
                action,
                sourceTable.getId(),
                sourceTable.getTableNumber(),
                buildTableDisplayName(sourceGroup, extractMergeRootId(resolveOperationalTable(sourceTable))),
                sourceTable.getStatus().name(),
                sourceOrder.getId(),
                sourceOrder.getStatus().name(),
                calculateSubtotal(sourceOrder),
                targetTable.getId(),
                targetTable.getTableNumber(),
                buildTableDisplayName(targetGroup, extractMergeRootId(resolveOperationalTable(targetTable))),
                targetTable.getStatus().name(),
                targetOrder.getId(),
                targetOrder.getStatus().name(),
                calculateSubtotal(targetOrder)
        );
    }

    private UnmergeTablesResponse buildUnmergeResponse(RestaurantTable rootTable,
                                                       Order rootOrder,
                                                       List<Order> targetOrders,
                                                       List<RestaurantTable> originalGroup) {
        Map<Integer, Order> orderByTableId = new HashMap<>();
        if (rootOrder.getStatus() != OrderStatus.CANCELLED) {
            orderByTableId.put(rootTable.getId(), rootOrder);
        }
        for (Order targetOrder : targetOrders) {
            if (targetOrder.getTable() != null) {
                orderByTableId.put(targetOrder.getTable().getId(), targetOrder);
            }
        }

        List<UnmergeTableResultResponse> tableSnapshots = originalGroup.stream()
                .sorted(TABLE_GROUP_COMPARATOR)
                .map(table -> {
                    Order activeOrder = orderByTableId.get(table.getId());
                    return new UnmergeTableResultResponse(
                            table.getId(),
                            table.getTableNumber(),
                            table.getTableNumber(),
                            table.getStatus().name(),
                            activeOrder != null ? activeOrder.getId() : null,
                            activeOrder != null ? activeOrder.getStatus().name() : null,
                            activeOrder != null ? calculateSubtotal(activeOrder) : BigDecimal.ZERO
                    );
                })
                .toList();

        return new UnmergeTablesResponse(
                "UNMERGE",
                rootTable.getId(),
                rootTable.getTableNumber(),
                rootTable.getTableNumber(),
                rootOrder.getId(),
                rootOrder.getStatus().name(),
                tableSnapshots
        );
    }

    private BigDecimal calculateSubtotal(Order order) {
        return order.getOrderItems().stream()
                .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderResponse toOrderResponse(Order order) {
        RestaurantTable rootTable = order.getTable() != null ? resolveOperationalTable(order.getTable()) : null;
        List<RestaurantTable> mergedGroup = rootTable != null ? loadMergedGroup(rootTable) : List.of();

        return new OrderResponse(
                order.getId(),
                order.getBranch().getId(),
                order.getBranch().getName(),
                rootTable != null ? rootTable.getId() : null,
                rootTable != null ? rootTable.getTableNumber() : null,
                rootTable != null ? buildTableDisplayName(mergedGroup, rootTable.getId()) : null,
                mergedGroup.stream().map(RestaurantTable::getId).toList(),
                mergedGroup.stream().map(RestaurantTable::getTableNumber).toList(),
                order.getCustomer() != null ? order.getCustomer().getId() : null,
                order.getCustomer() != null ? order.getCustomer().getName() : null,
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getClosedAt(),
                calculateSubtotal(order),
                order.getOrderItems().stream()
                        .map(this::toOrderItemResponse)
                        .toList(),
                toOrderItemSummaryResponses(order)
        );
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
        BigDecimal unitPrice = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;

        return new OrderItemResponse(
                item.getId(),
                item.getMenuItem().getId(),
                item.getMenuItem().getName(),
                unitPrice,
                quantity,
                item.getNote(),
                item.getStatus().name(),
                unitPrice.multiply(BigDecimal.valueOf(quantity))
        );
    }

    private List<OrderItemSummaryResponse> toOrderItemSummaryResponses(Order order) {
        Map<OrderItemSummaryKey, OrderItemSummaryAccumulator> groupedItems = new LinkedHashMap<>();

        order.getOrderItems().stream()
                .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
                .filter(item -> item.getMenuItem() != null)
                .forEach(item -> {
                    BigDecimal unitPrice = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                    String note = normalizeNote(item.getNote());
                    OrderItemSummaryKey key = new OrderItemSummaryKey(
                            item.getMenuItem().getId(),
                            item.getMenuItem().getName(),
                            unitPrice,
                            note
                    );
                    groupedItems.computeIfAbsent(key, OrderItemSummaryAccumulator::new).add(item);
                });

        return groupedItems.values().stream()
                .map(OrderItemSummaryAccumulator::toResponse)
                .toList();
    }

    private record OrderItemSummaryKey(
            Integer menuItemId,
            String menuItemName,
            BigDecimal unitPrice,
            String note
    ) {
    }

    private static class OrderItemSummaryAccumulator {
        private final OrderItemSummaryKey key;
        private int quantity;
        private int sentQuantity;
        private int preparingQuantity;
        private int readyQuantity;
        private int servedQuantity;
        private final List<Integer> orderItemIds = new ArrayList<>();
        private final List<Integer> readyOrderItemIds = new ArrayList<>();
        private final List<Integer> cancellableOrderItemIds = new ArrayList<>();

        private OrderItemSummaryAccumulator(OrderItemSummaryKey key) {
            this.key = key;
        }

        private void add(OrderItem item) {
            int itemQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
            quantity += itemQuantity;
            orderItemIds.add(item.getId());

            if (item.getStatus() == OrderItemStatus.PENDING || item.getStatus() == OrderItemStatus.WAITING_STOCK) {
                sentQuantity += itemQuantity;
                cancellableOrderItemIds.add(item.getId());
            } else if (item.getStatus() == OrderItemStatus.PROCESSING) {
                preparingQuantity += itemQuantity;
                cancellableOrderItemIds.add(item.getId());
            } else if (item.getStatus() == OrderItemStatus.COMPLETED) {
                readyQuantity += itemQuantity;
                readyOrderItemIds.add(item.getId());
            } else if (item.getStatus() == OrderItemStatus.SERVED) {
                servedQuantity += itemQuantity;
            }
        }

        private OrderItemSummaryResponse toResponse() {
            BigDecimal lineTotal = key.unitPrice().multiply(BigDecimal.valueOf(quantity));

            return new OrderItemSummaryResponse(
                    key.menuItemId(),
                    key.menuItemName(),
                    key.unitPrice(),
                    quantity,
                    key.note(),
                    lineTotal,
                    sentQuantity,
                    preparingQuantity,
                    readyQuantity,
                    servedQuantity,
                    List.copyOf(orderItemIds),
                    List.copyOf(readyOrderItemIds),
                    List.copyOf(cancellableOrderItemIds)
            );
        }
    }

    private RestaurantTable resolveOperationalTable(RestaurantTable table) {
        if (table == null || table.getMergedIntoTable() == null) {
            return table;
        }
        // Nếu đây là bàn thành viên thì truy ngược về bàn gốc hiện tại của nhóm.
        return resolveTable(table.getMergedIntoTable().getId());
    }

    private List<RestaurantTable> loadMergedGroup(RestaurantTable rootTable) {
        if (rootTable == null) {
            return List.of();
        }

        List<RestaurantTable> mergedGroup = restaurantTableRepository.findAllInMergedGroups(List.of(rootTable.getId()));
        if (mergedGroup.isEmpty()) {
            return List.of(rootTable);
        }
        mergedGroup.sort(TABLE_GROUP_COMPARATOR);
        return mergedGroup;
    }

    private void validateUnmergeSelections(RestaurantTable rootTable,
                                           List<RestaurantTable> mergedGroup,
                                           Order rootOrder,
                                           List<UnmergeTableTargetRequest> targets,
                                           CustomUserDetails currentUser) {
        Map<Integer, RestaurantTable> groupedMembers = mergedGroup.stream()
                .filter(table -> !table.getId().equals(rootTable.getId()))
                .collect(LinkedHashMap::new, (map, table) -> map.put(table.getId(), table), Map::putAll);

        Map<Integer, Integer> requestedQuantityByItemId = new HashMap<>();
        Set<Integer> seenTargetTableIds = new HashSet<>();

        for (UnmergeTableTargetRequest targetRequest : targets) {
            Integer targetTableId = targetRequest.targetTableId();
            if (!seenTargetTableIds.add(targetTableId)) {
                throw new ConflictException("Each member table can only appear once in an unmerge request");
            }
            RestaurantTable targetTable = groupedMembers.get(targetTableId);
            if (targetTable == null) {
                throw new ConflictException("Unmerge targets must be member tables of the selected merged group");
            }
            validateTableTransferScope(rootTable, targetTable, currentUser);

            if (targetRequest.items() == null || targetRequest.items().isEmpty()) {
                throw new ConflictException("Each target table must receive at least one dish");
            }

            for (SplitOrderItemRequest itemRequest : targetRequest.items()) {
                OrderItem sourceItem = rootOrder.getOrderItems().stream()
                        .filter(item -> item.getId().equals(itemRequest.orderItemId()))
                        .findFirst()
                        .orElseThrow(() -> new ConflictException("Selected order item does not belong to the merged root order"));

                if (sourceItem.getStatus() == OrderItemStatus.CANCELLED) {
                    throw new ConflictException("Cancelled dishes cannot be moved during unmerge");
                }

                int accumulatedQuantity = requestedQuantityByItemId.getOrDefault(itemRequest.orderItemId(), 0)
                        + itemRequest.quantity();
                if (accumulatedQuantity > sourceItem.getQuantity()) {
                    throw new ConflictException("Total unmerge quantity exceeds the source dish quantity");
                }
                requestedQuantityByItemId.put(itemRequest.orderItemId(), accumulatedQuantity);
            }
        }

        if (requestedQuantityByItemId.isEmpty()) {
            throw new ConflictException("At least one dish must be assigned back to a member table");
        }
    }

    private void ensureStandaloneTable(RestaurantTable table, String label) {
        // Một bàn không còn "độc lập" nếu:
        // - chính nó đang trỏ mergedIntoTable về bàn gốc khác
        // - hoặc nó là bàn gốc đang có các bàn thành viên
        if (table.getMergedIntoTable() != null || restaurantTableRepository.existsByMergedIntoTable_Id(table.getId())) {
            throw new ConflictException(label + " is already part of a merged-table group");
        }
    }

    private List<RestaurantTable> mergeDistinctTables(List<RestaurantTable> sourceGroup,
                                                      List<RestaurantTable> targetGroup) {
        Map<Integer, RestaurantTable> tableMap = new LinkedHashMap<>();
        sourceGroup.forEach(table -> tableMap.put(table.getId(), table));
        targetGroup.forEach(table -> tableMap.put(table.getId(), table));
        return new ArrayList<>(tableMap.values());
    }

    private void releaseMergedGroup(RestaurantTable rootTable,
                                    RestaurantTableStatus status,
                                    boolean clearMergeLinks) {
        RestaurantTable operationalRoot = resolveOperationalTable(rootTable);
        List<RestaurantTable> mergedGroup = loadMergedGroup(operationalRoot);
        for (RestaurantTable groupTable : mergedGroup) {
            groupTable.setStatus(status);
            if (clearMergeLinks && !groupTable.getId().equals(operationalRoot.getId())) {
                // Khi trả nhóm bàn về trạng thái độc lập, các bàn thành viên phải bỏ liên kết gộp.
                groupTable.setMergedIntoTable(null);
            }
        }
        restaurantTableRepository.saveAll(mergedGroup);
    }

    private Integer extractMergeRootId(RestaurantTable table) {
        return table.getMergedIntoTable() != null ? table.getMergedIntoTable().getId() : table.getId();
    }

    private String buildTableDisplayName(List<RestaurantTable> mergedGroup, Integer mergeRootId) {
        // Luôn ưu tiên bàn gốc đứng đầu để tên hiển thị ổn định giữa các lần gọi API.
        return mergedGroup.stream()
                .sorted((left, right) -> {
                    if (left.getId().equals(mergeRootId)) {
                        return -1;
                    }
                    if (right.getId().equals(mergeRootId)) {
                        return 1;
                    }
                    return TABLE_GROUP_COMPARATOR.compare(left, right);
                })
                .map(RestaurantTable::getTableNumber)
                .reduce((left, right) -> left + "&" + right)
                .orElse(null);
    }

    private String normalizeNote(String note) {
        return StringUtils.hasText(note) ? note.trim() : null;
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
