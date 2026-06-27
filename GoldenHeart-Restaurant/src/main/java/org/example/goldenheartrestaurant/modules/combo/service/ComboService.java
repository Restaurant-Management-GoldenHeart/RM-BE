package org.example.goldenheartrestaurant.modules.combo.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.combo.dto.request.ComboItemRequest;
import org.example.goldenheartrestaurant.modules.combo.dto.request.CreateComboRequest;
import org.example.goldenheartrestaurant.modules.combo.dto.request.UpdateComboRequest;
import org.example.goldenheartrestaurant.modules.combo.dto.response.ComboItemResponse;
import org.example.goldenheartrestaurant.modules.combo.dto.response.ComboResponse;
import org.example.goldenheartrestaurant.modules.combo.entity.Combo;
import org.example.goldenheartrestaurant.modules.combo.entity.ComboItem;
import org.example.goldenheartrestaurant.modules.combo.entity.ComboStatus;
import org.example.goldenheartrestaurant.modules.combo.repository.ComboRepository;
import org.example.goldenheartrestaurant.modules.identity.repository.UserProfileRepository;
import org.example.goldenheartrestaurant.modules.menu.entity.MenuItem;
import org.example.goldenheartrestaurant.modules.menu.repository.MenuItemRepository;
import org.example.goldenheartrestaurant.modules.menu.service.MenuItemImageStorageService;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.example.goldenheartrestaurant.modules.restaurant.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ComboService {

    private final ComboRepository comboRepository;
    private final MenuItemRepository menuItemRepository;
    private final BranchRepository branchRepository;
    private final UserProfileRepository userProfileRepository;
    private final MenuItemImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    public List<ComboResponse> listByBranch(Integer requestBranchId, CustomUserDetails currentUser) {
        Integer branchId = resolveBranchId(requestBranchId, currentUser);
        return comboRepository.findByBranchIdWithItems(branchId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComboResponse getById(Integer id, CustomUserDetails currentUser) {
        Combo combo = loadComboOrThrow(id);
        enforceReadScope(combo, currentUser);
        return toResponse(combo);
    }

    @Transactional
    public ComboResponse createCombo(CreateComboRequest request, MultipartFile imageFile, CustomUserDetails currentUser) {
        Integer branchId = resolveBranchId(request.branchId(), currentUser);
        Branch branch = resolveBranch(branchId);

        validateUniqueness(branchId, request.name(), null);

        List<MenuItem> menuItems = resolveMenuItems(request.items(), branchId);

        Combo combo = Combo.builder()
                .branch(branch)
                .name(request.name().trim())
                .description(request.description())
                .build();

        replaceItems(combo, request.items(), menuItems);
        combo.setPrice(calculatePrice(request.items(), menuItems, combo.getDiscountPct()));

        MenuItemImageStorageService.StoredMenuItemImage stored =
                imageStorageService.uploadMenuItemImage(imageFile, branchId, "combo-" + request.name());
        if (stored != null) {
            combo.setImageUrl(stored.imageUrl());
            combo.setImagePublicId(stored.imagePublicId());
        }

        try {
            return toResponse(comboRepository.save(combo));
        } catch (RuntimeException ex) {
            if (stored != null) imageStorageService.deleteMenuItemImage(stored.imagePublicId());
            throw ex;
        }
    }

    @Transactional
    public ComboResponse updateCombo(Integer id, UpdateComboRequest request, MultipartFile imageFile, CustomUserDetails currentUser) {
        Combo combo = loadComboOrThrow(id);
        enforceWriteScope(combo, currentUser);

        String nextName = StringUtils.hasText(request.name()) ? request.name().trim() : combo.getName();
        if (!nextName.equalsIgnoreCase(combo.getName())) {
            validateUniqueness(combo.getBranch().getId(), nextName, id);
        }

        combo.setName(nextName);

        if (request.description() != null) {
            combo.setDescription(request.description());
        }

        if (StringUtils.hasText(request.status())) {
            combo.setStatus(ComboStatus.valueOf(request.status().toUpperCase()));
        }

        if (request.items() != null) {
            if (request.items().isEmpty()) {
                throw new ConflictException("Combo phải có ít nhất một món");
            }
            List<MenuItem> menuItems = resolveMenuItems(request.items(), combo.getBranch().getId());
            replaceItems(combo, request.items(), menuItems);
            combo.setPrice(calculatePrice(request.items(), menuItems, combo.getDiscountPct()));
        }

        String previousImagePublicId = combo.getImagePublicId();
        MenuItemImageStorageService.StoredMenuItemImage stored =
                imageStorageService.uploadMenuItemImage(imageFile, combo.getBranch().getId(), "combo-" + nextName);
        if (stored != null) {
            combo.setImageUrl(stored.imageUrl());
            combo.setImagePublicId(stored.imagePublicId());
        }

        Combo saved;
        try {
            saved = comboRepository.save(combo);
        } catch (RuntimeException ex) {
            if (stored != null) imageStorageService.deleteMenuItemImage(stored.imagePublicId());
            throw ex;
        }

        if (stored != null && StringUtils.hasText(previousImagePublicId)
                && !previousImagePublicId.equals(stored.imagePublicId())) {
            imageStorageService.deleteMenuItemImage(previousImagePublicId);
        }

        return toResponse(saved);
    }

    @Transactional
    public void deleteCombo(Integer id, CustomUserDetails currentUser) {
        Combo combo = loadComboOrThrow(id);
        enforceWriteScope(combo, currentUser);
        comboRepository.delete(combo);
        if (StringUtils.hasText(combo.getImagePublicId())) {
            imageStorageService.deleteMenuItemImage(combo.getImagePublicId());
        }
    }

    // ─── Branch resolution ────────────────────────────────────────────────────

    /**
     * ADMIN: phải truyền branchId tường minh (lấy từ BranchContext trên FE).
     * MANAGER: luôn dùng branch của chính họ từ UserProfile — không cho phép tự chỉ định branch khác.
     */
    private Integer resolveBranchId(Integer requestBranchId, CustomUserDetails currentUser) {
        if ("ADMIN".equals(currentUser.getRoleName())) {
            if (requestBranchId == null) {
                throw new ConflictException("branchId là bắt buộc đối với ADMIN");
            }
            return requestBranchId;
        }
        // MANAGER: resolve từ profile, bỏ qua requestBranchId để tránh giả mạo branch
        return userProfileRepository.findActiveDetailByUserId(currentUser.getUserId())
                .map(p -> p.getBranch() != null ? p.getBranch().getId() : null)
                .filter(Objects::nonNull)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn chưa được gán chi nhánh"));
    }

    // ─── Scope enforcement ────────────────────────────────────────────────────

    private void enforceReadScope(Combo combo, CustomUserDetails currentUser) {
        if ("ADMIN".equals(currentUser.getRoleName())) return;
        Integer myBranchId = getBranchIdFromProfile(currentUser);
        if (!combo.getBranch().getId().equals(myBranchId)) {
            throw new ForbiddenException("Bạn không có quyền xem combo này");
        }
    }

    private void enforceWriteScope(Combo combo, CustomUserDetails currentUser) {
        if ("ADMIN".equals(currentUser.getRoleName())) return;
        Integer myBranchId = getBranchIdFromProfile(currentUser);
        if (!combo.getBranch().getId().equals(myBranchId)) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa combo này");
        }
    }

    private Integer getBranchIdFromProfile(CustomUserDetails currentUser) {
        return userProfileRepository.findActiveDetailByUserId(currentUser.getUserId())
                .map(p -> p.getBranch() != null ? p.getBranch().getId() : null)
                .filter(Objects::nonNull)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn chưa được gán chi nhánh"));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Combo loadComboOrThrow(Integer id) {
        return comboRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Combo không tồn tại"));
    }

    private Branch resolveBranch(Integer branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Chi nhánh không tồn tại"));
    }

    private List<MenuItem> resolveMenuItems(List<ComboItemRequest> itemRequests, Integer branchId) {
        List<MenuItem> result = new ArrayList<>();
        for (ComboItemRequest req : itemRequests) {
            MenuItem mi = menuItemRepository.findById(req.menuItemId())
                    .orElseThrow(() -> new NotFoundException("Món ăn không tồn tại: " + req.menuItemId()));
            if (!mi.getBranch().getId().equals(branchId)) {
                throw new ConflictException("Món ăn #" + req.menuItemId() + " không thuộc chi nhánh này");
            }
            result.add(mi);
        }
        return result;
    }

    private void replaceItems(Combo combo, List<ComboItemRequest> itemRequests, List<MenuItem> menuItems) {
        combo.getItems().clear();
        for (int i = 0; i < itemRequests.size(); i++) {
            combo.getItems().add(ComboItem.builder()
                    .combo(combo)
                    .menuItem(menuItems.get(i))
                    .quantity(itemRequests.get(i).quantity())
                    .build());
        }
    }

    private BigDecimal calculatePrice(List<ComboItemRequest> items, List<MenuItem> menuItems, int discountPct) {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < items.size(); i++) {
            BigDecimal itemTotal = menuItems.get(i).getPrice()
                    .multiply(BigDecimal.valueOf(items.get(i).quantity()));
            total = total.add(itemTotal);
        }
        BigDecimal multiplier = BigDecimal.ONE.subtract(
                BigDecimal.valueOf(discountPct).divide(BigDecimal.valueOf(100)));
        return total.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateUniqueness(Integer branchId, String name, Integer excludeId) {
        boolean exists = excludeId == null
                ? comboRepository.existsByBranchIdAndNameIgnoreCase(branchId, name)
                : comboRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(branchId, name, excludeId);
        if (exists) {
            throw new ConflictException("Combo với tên này đã tồn tại trong chi nhánh");
        }
    }

    private ComboResponse toResponse(Combo combo) {
        return ComboResponse.builder()
                .id(combo.getId())
                .branchId(combo.getBranch().getId())
                .branchName(combo.getBranch().getName())
                .name(combo.getName())
                .description(combo.getDescription())
                .imageUrl(combo.getImageUrl())
                .price(combo.getPrice())
                .discountPct(combo.getDiscountPct())
                .status(combo.getStatus().name())
                .items(combo.getItems().stream()
                        .map(ci -> ComboItemResponse.builder()
                                .menuItemId(ci.getMenuItem().getId())
                                .menuItemName(ci.getMenuItem().getName())
                                .menuItemImageUrl(ci.getMenuItem().getImageUrl())
                                .menuItemPrice(ci.getMenuItem().getPrice())
                                .quantity(ci.getQuantity())
                                .build())
                        .toList())
                .build();
    }
}
