package org.example.goldenheartrestaurant.modules.restaurant.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.identity.repository.UserProfileRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.InventoryRepository;
import org.example.goldenheartrestaurant.modules.menu.repository.MenuItemRepository;
import org.example.goldenheartrestaurant.modules.order.repository.OrderRepository;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.CreateBranchRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.UpdateBranchRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.response.BranchResponse;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Restaurant;
import org.example.goldenheartrestaurant.modules.restaurant.repository.BranchRepository;
import org.example.goldenheartrestaurant.modules.restaurant.repository.DiningAreaRepository;
import org.example.goldenheartrestaurant.modules.restaurant.repository.RestaurantRepository;
import org.example.goldenheartrestaurant.modules.restaurant.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final BranchRepository branchRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserProfileRepository userProfileRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final DiningAreaRepository diningAreaRepository;
    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public List<BranchResponse> getBranches(Integer restaurantId, String keyword) {
        return branchRepository.findAllForListing(restaurantId, normalizeKeyword(keyword))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchResponse getBranchById(Integer branchId) {
        return toResponse(getBranchOrThrow(branchId));
    }

    @Transactional
    public BranchResponse createBranch(CreateBranchRequest request, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN);

        Restaurant restaurant = resolveRestaurant(request.restaurantId());
        String normalizedName = normalizeName(request.name());
        ensureBranchNameAvailable(restaurant.getId(), normalizedName, null);

        Branch branch = Branch.builder()
                .restaurant(restaurant)
                .name(normalizedName)
                .address(normalizeText(request.address()))
                .phone(normalizePhone(request.phone()))
                .build();

        return toResponse(branchRepository.save(branch));
    }

    @Transactional
    public BranchResponse updateBranch(Integer branchId,
                                       UpdateBranchRequest request,
                                       CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN);

        Branch branch = getBranchOrThrow(branchId);
        Restaurant restaurant = resolveRestaurant(request.restaurantId());
        String normalizedName = normalizeName(request.name());
        ensureBranchNameAvailable(restaurant.getId(), normalizedName, branchId);

        branch.setRestaurant(restaurant);
        branch.setName(normalizedName);
        branch.setAddress(normalizeText(request.address()));
        branch.setPhone(normalizePhone(request.phone()));

        return toResponse(branchRepository.save(branch));
    }

    @Transactional
    public void deleteBranch(Integer branchId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN);

        Branch branch = getBranchOrThrow(branchId);
        ensureBranchCanBeDeleted(branchId);
        branchRepository.delete(branch);
    }

    private Branch getBranchOrThrow(Integer branchId) {
        return branchRepository.findDetailById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found"));
    }

    private Restaurant resolveRestaurant(Integer restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("Restaurant not found"));
    }

    private void ensureBranchNameAvailable(Integer restaurantId, String name, Integer currentBranchId) {
        boolean exists = currentBranchId == null
                ? branchRepository.existsByRestaurant_IdAndNameIgnoreCase(restaurantId, name)
                : branchRepository.existsByRestaurant_IdAndNameIgnoreCaseAndIdNot(restaurantId, name, currentBranchId);

        if (exists) {
            throw new ConflictException("Branch name already exists in the selected restaurant");
        }
    }

    private void ensureBranchCanBeDeleted(Integer branchId) {
        if (userProfileRepository.existsByBranch_Id(branchId)) {
            throw new ConflictException("Cannot delete branch because users are still assigned to it");
        }
        if (restaurantTableRepository.existsByBranch_Id(branchId)) {
            throw new ConflictException("Cannot delete branch because tables still belong to it");
        }
        if (diningAreaRepository.existsByBranch_Id(branchId)) {
            throw new ConflictException("Cannot delete branch because dining areas still belong to it");
        }
        if (orderRepository.existsByBranch_Id(branchId)) {
            throw new ConflictException("Cannot delete branch because it already has order history");
        }
        if (menuItemRepository.existsByBranchId(branchId)) {
            throw new ConflictException("Cannot delete branch because menu items still belong to it");
        }
        if (inventoryRepository.existsByBranchId(branchId)) {
            throw new ConflictException("Cannot delete branch because inventory items still belong to it");
        }
    }

    private BranchResponse toResponse(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getRestaurant().getId(),
                branch.getRestaurant().getName(),
                branch.getName(),
                branch.getAddress(),
                branch.getPhone()
        );
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizePhone(String phone) {
        return StringUtils.hasText(phone) ? phone.trim() : null;
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
