package org.example.goldenheartrestaurant.modules.restaurant.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.identity.entity.UserProfile;
import org.example.goldenheartrestaurant.modules.identity.repository.UserProfileRepository;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.CreateDiningAreaRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.UpdateDiningAreaRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.response.DiningAreaResponse;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.example.goldenheartrestaurant.modules.restaurant.entity.DiningArea;
import org.example.goldenheartrestaurant.modules.restaurant.repository.BranchRepository;
import org.example.goldenheartrestaurant.modules.restaurant.repository.DiningAreaRepository;
import org.example.goldenheartrestaurant.modules.restaurant.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * Service CRUD cho khu vuc ban.
 *
 * Rule nghiep vu:
 * - ADMIN: toan quyen
 * - MANAGER: chi tao/sua/xem trong branch cua minh
 * - STAFF/KITCHEN: chi duoc xem trong branch cua minh
 */
public class DiningAreaService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_STAFF = "STAFF";
    private static final String ROLE_KITCHEN = "KITCHEN";

    private final DiningAreaRepository diningAreaRepository;
    private final BranchRepository branchRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public List<DiningAreaResponse> getDiningAreas(Integer branchId,
                                                   Boolean active,
                                                   String keyword,
                                                   CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF, ROLE_KITCHEN);

        Integer scopedBranchId = resolveReadableBranchId(branchId, currentUser);
        String normalizedKeyword = normalizeKeyword(keyword);

        return diningAreaRepository.findAllForListing(scopedBranchId, active, normalizedKeyword)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DiningAreaResponse getDiningAreaById(Integer areaId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF, ROLE_KITCHEN);

        DiningArea area = getDiningAreaEntity(areaId);
        Integer scopedBranchId = resolveReadableBranchId(area.getBranch().getId(), currentUser);
        if (!area.getBranch().getId().equals(scopedBranchId)) {
            throw new ForbiddenException("You do not have permission to view this dining area");
        }

        return toResponse(area);
    }

    @Transactional
    public DiningAreaResponse createDiningArea(CreateDiningAreaRequest request, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER);

        Integer scopedBranchId = resolveWritableBranchId(request.branchId(), currentUser);
        Branch branch = resolveBranch(scopedBranchId);
        String normalizedName = normalizeName(request.name());
        String normalizedCode = normalizeCode(request.code());

        ensureAreaNameAvailable(scopedBranchId, normalizedName, null);
        ensureAreaCodeAvailable(scopedBranchId, normalizedCode, null);

        DiningArea area = DiningArea.builder()
                .branch(branch)
                .name(normalizedName)
                .code(normalizedCode)
                .displayOrder(request.displayOrder())
                .active(request.active() != null ? request.active() : Boolean.TRUE)
                .build();

        return toResponse(diningAreaRepository.save(area));
    }

    @Transactional
    public DiningAreaResponse updateDiningArea(Integer areaId,
                                               UpdateDiningAreaRequest request,
                                               CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER);

        DiningArea area = getDiningAreaEntity(areaId);
        Integer scopedBranchId = resolveWritableBranchId(request.branchId(), currentUser);
        boolean movingToAnotherBranch = !area.getBranch().getId().equals(scopedBranchId);

        if (movingToAnotherBranch && restaurantTableRepository.existsByArea_Id(areaId)) {
            throw new ConflictException("Cannot move a dining area to another branch while tables still belong to it");
        }

        Branch branch = resolveBranch(scopedBranchId);
        String normalizedName = normalizeName(request.name());
        String normalizedCode = normalizeCode(request.code());

        ensureAreaNameAvailable(scopedBranchId, normalizedName, areaId);
        ensureAreaCodeAvailable(scopedBranchId, normalizedCode, areaId);

        area.setBranch(branch);
        area.setName(normalizedName);
        area.setCode(normalizedCode);
        area.setDisplayOrder(request.displayOrder());
        area.setActive(request.active());

        return toResponse(diningAreaRepository.save(area));
    }

    @Transactional
    public void deleteDiningArea(Integer areaId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN);

        DiningArea area = getDiningAreaEntity(areaId);
        if (restaurantTableRepository.existsByArea_Id(areaId)) {
            throw new ConflictException("Cannot delete a dining area that still has tables");
        }

        diningAreaRepository.delete(area);
    }

    @Transactional(readOnly = true)
    public DiningArea getDiningAreaEntity(Integer areaId) {
        return diningAreaRepository.findDetailById(areaId)
                .orElseThrow(() -> new NotFoundException("Dining area not found"));
    }

    private Branch resolveBranch(Integer branchId) {
        return branchRepository.findDetailById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found"));
    }

    private void ensureAreaNameAvailable(Integer branchId, String name, Integer currentAreaId) {
        boolean exists = currentAreaId == null
                ? diningAreaRepository.existsByBranch_IdAndNameIgnoreCase(branchId, name)
                : diningAreaRepository.existsByBranch_IdAndNameIgnoreCaseAndIdNot(branchId, name, currentAreaId);
        if (exists) {
            throw new ConflictException("Dining area name already exists in the selected branch");
        }
    }

    private void ensureAreaCodeAvailable(Integer branchId, String code, Integer currentAreaId) {
        boolean exists = currentAreaId == null
                ? diningAreaRepository.existsByBranch_IdAndCodeIgnoreCase(branchId, code)
                : diningAreaRepository.existsByBranch_IdAndCodeIgnoreCaseAndIdNot(branchId, code, currentAreaId);
        if (exists) {
            throw new ConflictException("Dining area code already exists in the selected branch");
        }
    }

    private Integer resolveReadableBranchId(Integer branchId, CustomUserDetails currentUser) {
        if (hasRole(currentUser, ROLE_ADMIN)) {
            return branchId;
        }

        Integer ownBranchId = getAssignedBranchId(currentUser);
        if (branchId != null && !branchId.equals(ownBranchId)) {
            throw new ForbiddenException("You do not have permission to view another branch");
        }
        return ownBranchId;
    }

    private Integer resolveWritableBranchId(Integer branchId, CustomUserDetails currentUser) {
        if (branchId == null) {
            throw new ConflictException("Branch is required");
        }

        if (hasRole(currentUser, ROLE_ADMIN)) {
            return branchId;
        }

        Integer ownBranchId = getAssignedBranchId(currentUser);
        if (!branchId.equals(ownBranchId)) {
            throw new ForbiddenException("You do not have permission to manage another branch");
        }
        return ownBranchId;
    }

    private Integer getAssignedBranchId(CustomUserDetails currentUser) {
        UserProfile profile = userProfileRepository.findActiveDetailByUserId(currentUser.getUserId())
                .orElseThrow(() -> new NotFoundException("Current user profile not found"));

        if (profile.getBranch() == null) {
            throw new ForbiddenException("Your account is not assigned to any branch");
        }

        return profile.getBranch().getId();
    }

    private DiningAreaResponse toResponse(DiningArea area) {
        return new DiningAreaResponse(
                area.getId(),
                area.getBranch().getId(),
                area.getBranch().getName(),
                area.getName(),
                area.getCode(),
                area.getDisplayOrder(),
                area.getActive(),
                area.getTables() != null ? area.getTables().size() : 0
        );
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
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
