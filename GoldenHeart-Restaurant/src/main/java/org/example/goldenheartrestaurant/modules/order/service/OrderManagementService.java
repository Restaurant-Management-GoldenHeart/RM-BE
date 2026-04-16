package org.example.goldenheartrestaurant.modules.order.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.modules.billing.repository.BillRepository;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.customer.entity.Customer;
import org.example.goldenheartrestaurant.modules.customer.repository.CustomerRepository;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.example.goldenheartrestaurant.modules.menu.entity.MenuItem;
import org.example.goldenheartrestaurant.modules.menu.entity.MenuItemStatus;
import org.example.goldenheartrestaurant.modules.menu.repository.MenuItemRepository;
import org.example.goldenheartrestaurant.modules.order.dto.request.CreateOrderRequest;
import org.example.goldenheartrestaurant.modules.order.dto.request.OrderItemRequest;
import org.example.goldenheartrestaurant.modules.order.dto.response.OrderItemResponse;
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
import org.example.goldenheartrestaurant.modules.restaurant.dto.response.TableOrderTransferResponse;
import org.example.goldenheartrestaurant.modules.restaurant.repository.BranchRepository;
import org.example.goldenheartrestaurant.modules.restaurant.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderManagementService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_STAFF = "STAFF";
    private static final String ROLE_KITCHEN = "KITCHEN";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BillRepository billRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final BranchRepository branchRepository;
    private final MenuItemRepository menuItemRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF);

        User actor = getCurrentUser(currentUser.getUserId());
        RestaurantTable table = resolveTable(request.tableId());
        Integer branchId = resolveBranchId(request.branchId(), table);
        enforceBranchScope(actor, currentUser, branchId);

        Branch branch = resolveBranch(branchId);
        Customer customer = resolveCustomer(request.customerId());

        Order order = table != null ? findActiveOrderEntityByTableId(table.getId()).orElse(null) : null;

        if (order == null) {
            validateTableCanOpenOrder(table);
            order = Order.builder()
                    .branch(branch)
                    .table(table)
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

        if (table != null) {
            table.setStatus(RestaurantTableStatus.OCCUPIED);
            restaurantTableRepository.save(table);
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
        return orderRepository.findActiveOrdersByTableId(tableId)
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

        RestaurantTable sourceTable = resolveTable(request.sourceTableId());
        RestaurantTable targetTable = resolveTable(request.targetTableId());
        validateTableTransferScope(sourceTable, targetTable, currentUser);

        Order sourceOrder = findActiveOrderEntityByTableId(sourceTable.getId())
                .orElseThrow(() -> new ConflictException("Source table does not have an active order"));
        Order targetOrder = findActiveOrderEntityByTableId(targetTable.getId())
                .orElseThrow(() -> new ConflictException("Target table does not have an active order"));

        ensureOrderHasNoBilling(sourceOrder);
        ensureOrderHasNoBilling(targetOrder);

        if (countBillableQuantity(sourceOrder) < 1 || countBillableQuantity(targetOrder) < 1) {
            throw new ConflictException("Both tables must each have at least one dish before merging");
        }

        List<OrderItem> itemsToMove = List.copyOf(sourceOrder.getOrderItems());
        for (OrderItem item : itemsToMove) {
            targetOrder.getOrderItems().add(item);
            item.setOrder(targetOrder);
            sourceOrder.getOrderItems().remove(item);
        }

        if (targetOrder.getCustomer() == null) {
            targetOrder.setCustomer(sourceOrder.getCustomer());
        }

        sourceOrder.setStatus(OrderStatus.CANCELLED);
        sourceOrder.setClosedAt(LocalDateTime.now());
        sourceTable.setStatus(RestaurantTableStatus.AVAILABLE);
        targetTable.setStatus(RestaurantTableStatus.OCCUPIED);

        recalculateOrderStatus(targetOrder);
        orderRepository.save(sourceOrder);
        Order savedTargetOrder = orderRepository.save(targetOrder);
        restaurantTableRepository.save(sourceTable);
        restaurantTableRepository.save(targetTable);

        return buildTableTransferResponse("MERGE", sourceTable, sourceOrder, targetTable, loadOrder(savedTargetOrder.getId()));
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
        if (menuItem.getStatus() != MenuItemStatus.AVAILABLE) {
            throw new ConflictException("Menu item is not available for ordering");
        }

        return menuItem;
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

    private TableOrderTransferResponse buildTableTransferResponse(String action,
                                                                 RestaurantTable sourceTable,
                                                                 Order sourceOrder,
                                                                 RestaurantTable targetTable,
                                                                 Order targetOrder) {
        return new TableOrderTransferResponse(
                action,
                sourceTable.getId(),
                sourceTable.getTableNumber(),
                sourceTable.getStatus().name(),
                sourceOrder.getId(),
                sourceOrder.getStatus().name(),
                calculateSubtotal(sourceOrder),
                targetTable.getId(),
                targetTable.getTableNumber(),
                targetTable.getStatus().name(),
                targetOrder.getId(),
                targetOrder.getStatus().name(),
                calculateSubtotal(targetOrder)
        );
    }

    private BigDecimal calculateSubtotal(Order order) {
        return order.getOrderItems().stream()
                .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getBranch().getId(),
                order.getBranch().getName(),
                order.getTable() != null ? order.getTable().getId() : null,
                order.getTable() != null ? order.getTable().getTableNumber() : null,
                order.getCustomer() != null ? order.getCustomer().getId() : null,
                order.getCustomer() != null ? order.getCustomer().getName() : null,
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getClosedAt(),
                calculateSubtotal(order),
                order.getOrderItems().stream()
                        .map(item -> new OrderItemResponse(
                                item.getId(),
                                item.getMenuItem().getId(),
                                item.getMenuItem().getName(),
                                item.getPrice(),
                                item.getQuantity(),
                                item.getNote(),
                                item.getStatus().name(),
                                item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                        ))
                        .toList()
        );
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
