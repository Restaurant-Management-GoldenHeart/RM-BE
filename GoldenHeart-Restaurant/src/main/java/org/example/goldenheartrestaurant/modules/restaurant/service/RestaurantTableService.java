package org.example.goldenheartrestaurant.modules.restaurant.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.example.goldenheartrestaurant.modules.order.service.OrderManagementService;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.UpdateTableStatusRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.response.RestaurantTableResponse;
import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTable;
import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTableStatus;
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
