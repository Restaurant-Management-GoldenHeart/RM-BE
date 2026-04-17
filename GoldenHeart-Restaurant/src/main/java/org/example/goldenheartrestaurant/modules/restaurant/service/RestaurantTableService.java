package org.example.goldenheartrestaurant.modules.restaurant.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.example.goldenheartrestaurant.modules.order.repository.OrderRepository;
import org.example.goldenheartrestaurant.modules.order.service.OrderManagementService;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.CreateRestaurantTableRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.UpdateTableStatusRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.UpdateRestaurantTableRequest;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.example.goldenheartrestaurant.modules.restaurant.entity.DiningArea;
import org.example.goldenheartrestaurant.modules.restaurant.dto.response.RestaurantTableResponse;
import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTable;
import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTableStatus;
import org.example.goldenheartrestaurant.modules.restaurant.repository.BranchRepository;
import org.example.goldenheartrestaurant.modules.restaurant.repository.DiningAreaRepository;
import org.example.goldenheartrestaurant.modules.restaurant.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantTableService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_STAFF = "STAFF";
    private static final String ROLE_KITCHEN = "KITCHEN";

    private final RestaurantTableRepository restaurantTableRepository;
    private final BranchRepository branchRepository;
    private final DiningAreaRepository diningAreaRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderManagementService orderManagementService;

    @Transactional(readOnly = true)
    public List<RestaurantTableResponse> getTables(Integer branchId,
                                                   String status,
                                                   String keyword,
                                                   CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF, ROLE_KITCHEN);

        Integer scopedBranchId = resolveAccessibleBranchId(branchId, currentUser);
        RestaurantTableStatus statusFilter = parseStatus(status);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        return restaurantTableRepository.findAllForListing(scopedBranchId, statusFilter, normalizedKeyword)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RestaurantTableResponse getTableById(Integer tableId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF, ROLE_KITCHEN);

        RestaurantTable table = getTableEntity(tableId);
        Integer scopedBranchId = resolveAccessibleBranchId(table.getBranch().getId(), currentUser);
        if (!table.getBranch().getId().equals(scopedBranchId)) {
            throw new ForbiddenException("You do not have permission to view this table");
        }
        return toResponse(table);
    }

    @Transactional
    public RestaurantTableResponse createTable(CreateRestaurantTableRequest request, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER);

        Integer scopedBranchId = resolveAccessibleBranchId(request.branchId(), currentUser);
        Branch branch = resolveBranch(scopedBranchId);
        DiningArea area = resolveArea(request.areaId(), scopedBranchId);
        String normalizedTableNumber = normalizeTableNumber(request.tableNumber());
        ensureTableNumberAvailable(scopedBranchId, normalizedTableNumber, null);

        RestaurantTable table = RestaurantTable.builder()
                .branch(branch)
                .area(area)
                .tableNumber(normalizedTableNumber)
                .capacity(request.capacity())
                .posX(request.posX())
                .posY(request.posY())
                .width(request.width())
                .height(request.height())
                .displayOrder(request.displayOrder())
                .status(RestaurantTableStatus.AVAILABLE)
                .build();

        return toResponse(restaurantTableRepository.save(table));
    }

    @Transactional
    public RestaurantTableResponse updateTable(Integer tableId,
                                               UpdateRestaurantTableRequest request,
                                               CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER);

        RestaurantTable table = getTableEntity(tableId);
        Integer scopedBranchId = resolveAccessibleBranchId(request.branchId(), currentUser);
        boolean hasOrderHistory = orderRepository.existsByTable_Id(tableId);

        if (orderManagementService.findActiveOrderEntityByTableId(tableId).isPresent()) {
            throw new ConflictException("Cannot edit a table that still has an active order");
        }
        if (hasOrderHistory && !table.getBranch().getId().equals(scopedBranchId)) {
            throw new ConflictException("Cannot move a table to another branch after it already has order history");
        }

        Branch branch = resolveBranch(scopedBranchId);
        DiningArea area = resolveArea(request.areaId(), scopedBranchId);
        String normalizedTableNumber = normalizeTableNumber(request.tableNumber());
        ensureTableNumberAvailable(scopedBranchId, normalizedTableNumber, tableId);

        table.setBranch(branch);
        table.setArea(area);
        table.setTableNumber(normalizedTableNumber);
        table.setCapacity(request.capacity());
        table.setPosX(request.posX());
        table.setPosY(request.posY());
        table.setWidth(request.width());
        table.setHeight(request.height());
        table.setDisplayOrder(request.displayOrder());

        return toResponse(restaurantTableRepository.save(table));
    }

    @Transactional
    public void deleteTable(Integer tableId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN);

        RestaurantTable table = getTableEntity(tableId);
        Integer scopedBranchId = resolveAccessibleBranchId(table.getBranch().getId(), currentUser);
        if (!table.getBranch().getId().equals(scopedBranchId)) {
            throw new ForbiddenException("You do not have permission to delete this table");
        }
        if (orderManagementService.findActiveOrderEntityByTableId(tableId).isPresent()) {
            throw new ConflictException("Cannot delete a table that still has an active order");
        }
        if (orderRepository.existsByTable_Id(tableId)) {
            throw new ConflictException("Cannot delete a table that already has order history");
        }

        restaurantTableRepository.delete(table);
    }

    @Transactional
    public RestaurantTableResponse updateTableStatus(Integer tableId,
                                                     UpdateTableStatusRequest request,
                                                     CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF);

        RestaurantTable table = getTableEntity(tableId);
        Integer scopedBranchId = resolveAccessibleBranchId(table.getBranch().getId(), currentUser);
        if (!table.getBranch().getId().equals(scopedBranchId)) {
            throw new ForbiddenException("You do not have permission to update this table");
        }
        if (orderManagementService.findActiveOrderEntityByTableId(tableId).isPresent()) {
            throw new ConflictException("Cannot manually change a table that still has an active order");
        }

        RestaurantTableStatus targetStatus = parseRequiredStatus(request.status());
        validateStatusTransition(table.getStatus(), targetStatus);

        table.setStatus(targetStatus);
        restaurantTableRepository.save(table);
        return toResponse(table);
    }

    @Transactional(readOnly = true)
    public RestaurantTable getTableEntity(Integer tableId) {
        return restaurantTableRepository.findDetailById(tableId)
                .orElseThrow(() -> new NotFoundException("Table not found"));
    }

    private Branch resolveBranch(Integer branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found"));
    }

    private DiningArea resolveArea(Integer areaId, Integer branchId) {
        if (areaId == null) {
            return null;
        }

        DiningArea area = diningAreaRepository.findByIdAndBranch_Id(areaId, branchId)
                .orElseThrow(() -> new ConflictException("Dining area does not belong to the selected branch"));
        if (!Boolean.TRUE.equals(area.getActive())) {
            throw new ConflictException("Cannot assign a table to an inactive dining area");
        }
        return area;
    }

    private void ensureTableNumberAvailable(Integer branchId, String tableNumber, Integer currentTableId) {
        boolean exists = currentTableId == null
                ? restaurantTableRepository.existsByBranch_IdAndTableNumberIgnoreCase(branchId, tableNumber)
                : restaurantTableRepository.existsByBranch_IdAndTableNumberIgnoreCaseAndIdNot(branchId, tableNumber, currentTableId);

        if (exists) {
            throw new ConflictException("Table number already exists in the selected branch");
        }
    }

    private void validateStatusTransition(RestaurantTableStatus currentStatus, RestaurantTableStatus targetStatus) {
        if (currentStatus == targetStatus) {
            throw new ConflictException("Table is already in the target status");
        }
        if (targetStatus == RestaurantTableStatus.OCCUPIED) {
            throw new ConflictException("Occupied status is managed by order workflow");
        }
        if (currentStatus == RestaurantTableStatus.AVAILABLE && targetStatus == RestaurantTableStatus.RESERVED) {
            return;
        }
        if (currentStatus == RestaurantTableStatus.RESERVED && targetStatus == RestaurantTableStatus.AVAILABLE) {
            return;
        }
        if (currentStatus == RestaurantTableStatus.CLEANING && targetStatus == RestaurantTableStatus.AVAILABLE) {
            return;
        }

        throw new ConflictException("Unsupported table status transition");
    }

    private Integer resolveAccessibleBranchId(Integer branchId, CustomUserDetails currentUser) {
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
            throw new ForbiddenException("You do not have permission to view another branch");
        }

        return ownBranchId;
    }

    private RestaurantTableResponse toResponse(RestaurantTable table) {
        return new RestaurantTableResponse(
                table.getId(),
                table.getBranch().getId(),
                table.getBranch().getName(),
                table.getArea() != null ? table.getArea().getId() : null,
                table.getArea() != null ? table.getArea().getName() : null,
                table.getTableNumber(),
                table.getCapacity(),
                table.getPosX(),
                table.getPosY(),
                table.getWidth(),
                table.getHeight(),
                table.getDisplayOrder(),
                table.getStatus().name()
        );
    }

    private RestaurantTableStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return parseRequiredStatus(status);
    }

    private RestaurantTableStatus parseRequiredStatus(String status) {
        try {
            return RestaurantTableStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Unsupported table status");
        }
    }

    private String normalizeTableNumber(String tableNumber) {
        if (!StringUtils.hasText(tableNumber)) {
            throw new ConflictException("Table number is required");
        }
        return tableNumber.trim();
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
