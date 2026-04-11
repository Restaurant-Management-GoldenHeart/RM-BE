package org.example.goldenheartrestaurant.modules.inventory.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.example.goldenheartrestaurant.modules.inventory.dto.request.CreateInventoryItemRequest;
import org.example.goldenheartrestaurant.modules.inventory.dto.request.UpdateInventoryItemRequest;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventoryActionLogResponse;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventoryAlertLevel;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventoryAlertResponse;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventoryItemResponse;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.MeasurementUnitResponse;
import org.example.goldenheartrestaurant.modules.inventory.entity.Ingredient;
import org.example.goldenheartrestaurant.modules.inventory.entity.Inventory;
import org.example.goldenheartrestaurant.modules.inventory.entity.InventoryActionLog;
import org.example.goldenheartrestaurant.modules.inventory.entity.InventoryActionType;
import org.example.goldenheartrestaurant.modules.inventory.entity.MeasurementUnit;
import org.example.goldenheartrestaurant.modules.inventory.entity.StockMovement;
import org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType;
import org.example.goldenheartrestaurant.modules.inventory.repository.IngredientRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.InventoryActionLogRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.InventoryRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.MeasurementUnitRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.StockMovementRepository;
import org.example.goldenheartrestaurant.modules.menu.repository.RecipeRepository;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.example.goldenheartrestaurant.modules.restaurant.repository.BranchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
/**
 * Service xử lý nghiệp vụ inventory.
 *
 * Đây là một trong những service nhiều rule nhất của dự án:
 * - tạo mới inventory item đồng thời gắn / tạo Ingredient master
 * - chặn trùng branch + ingredient
 * - quản lý min stock / reorder level / average cost
 * - ghi stock movement khi quantity thay đổi
 * - ghi inventory action log để audit người thao tác
 * - chặn đổi đơn vị nếu ingredient đã đi vào recipe hoặc lịch sử kho
 * - chặn xóa nếu tồn còn > 0 hoặc ingredient vẫn được recipe sử dụng
 */
public class InventoryManagementService {

    private final InventoryRepository inventoryRepository;
    private final IngredientRepository ingredientRepository;
    private final MeasurementUnitRepository measurementUnitRepository;
    private final InventoryActionLogRepository inventoryActionLogRepository;
    private final StockMovementRepository stockMovementRepository;
    private final RecipeRepository recipeRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MeasurementUnitResponse> getMeasurementUnits() {
        return measurementUnitRepository.findAll().stream()
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .map(this::toMeasurementUnitResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> getInventoryItems(String keyword, Integer branchId, boolean lowStockOnly, int page, int size) {
        Page<Inventory> inventoryPage = inventoryRepository.search(normalizeKeyword(keyword), branchId, lowStockOnly, PageRequest.of(page, size));

        return PageResponse.<InventoryItemResponse>builder()
                .content(inventoryPage.getContent().stream().map(this::toInventoryItemResponse).toList())
                .page(inventoryPage.getNumber())
                .size(inventoryPage.getSize())
                .totalElements(inventoryPage.getTotalElements())
                .totalPages(inventoryPage.getTotalPages())
                .last(inventoryPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse getInventoryItemById(Integer inventoryId) {
        return toInventoryItemResponse(getInventoryOrThrow(inventoryId));
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryActionLogResponse> getInventoryHistory(Integer inventoryId, int page, int size) {
        ensureInventoryExists(inventoryId);

        Page<InventoryActionLog> historyPage = inventoryActionLogRepository.findByInventoryIdOrderByOccurredAtDesc(inventoryId, PageRequest.of(page, size));

        return PageResponse.<InventoryActionLogResponse>builder()
                .content(historyPage.getContent().stream().map(this::toInventoryActionLogResponse).toList())
                .page(historyPage.getNumber())
                .size(historyPage.getSize())
                .totalElements(historyPage.getTotalElements())
                .totalPages(historyPage.getTotalPages())
                .last(historyPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<InventoryAlertResponse> getLowStockAlerts(Integer branchId) {
        return inventoryRepository.findLowStockAlerts(branchId).stream()
                .map(this::toInventoryAlertResponse)
                .toList();
    }

    @Transactional
    public InventoryItemResponse createInventoryItem(CreateInventoryItemRequest request, CustomUserDetails currentUser) {
        // reorder level luôn phải >= min stock level.
        validateThresholds(request.minStockLevel(), request.reorderLevel());

        Branch branch = resolveBranch(request.branchId());
        MeasurementUnit unit = resolveMeasurementUnit(request.unitId());
        User actor = resolveActor(currentUser.getUserId());
        Ingredient ingredient = resolveOrCreateIngredient(request.ingredientName(), unit);

        if (inventoryRepository.findByBranchIdAndIngredientId(branch.getId(), ingredient.getId()).isPresent()) {
            // Cùng 1 chi nhánh không được có 2 inventory active cho cùng 1 ingredient.
            throw new ConflictException("Inventory item already exists for this branch and ingredient");
        }

        Inventory inventory = Inventory.builder()
                .branch(branch)
                .ingredient(ingredient)
                .quantity(defaultIfNull(request.quantity()))
                .minStockLevel(defaultIfNull(request.minStockLevel()))
                .reorderLevel(defaultIfNull(request.reorderLevel()))
                .averageUnitCost(defaultIfNull(request.averageUnitCost()))
                .lastReceiptAt(request.quantity().compareTo(BigDecimal.ZERO) > 0 ? LocalDateTime.now() : null)
                .build();

        Inventory savedInventory = inventoryRepository.save(inventory);

        // Nếu quantity ban đầu > 0 thì coi như một đợt nhập / điều chỉnh vào kho,
        // nên phải có stock movement để sau này còn truy vết.
        createStockMovementForManualChange(
                savedInventory,
                actor,
                BigDecimal.ZERO,
                savedInventory.getQuantity(),
                request.note(),
                "Tao moi nguyen lieu trong ton kho"
        );

        inventoryActionLogRepository.save(
                InventoryActionLog.builder()
                        .inventoryId(savedInventory.getId())
                        .branchId(branch.getId())
                        .branchName(branch.getName())
                        .ingredientId(ingredient.getId())
                        .ingredientName(ingredient.getName())
                        .unitSymbol(ingredient.resolveUnitSymbol())
                        .actedBy(actor)
                        .actedByUsername(actor.getUsername())
                        .actedByFullName(actor.getProfile() != null ? actor.getProfile().getFullName() : null)
                        .actionType(InventoryActionType.CREATED)
                        .beforeQuantity(BigDecimal.ZERO)
                        .afterQuantity(savedInventory.getQuantity())
                        .beforeMinStockLevel(BigDecimal.ZERO)
                        .afterMinStockLevel(savedInventory.getMinStockLevel())
                        .beforeReorderLevel(BigDecimal.ZERO)
                        .afterReorderLevel(savedInventory.getReorderLevel())
                        .beforeAverageUnitCost(BigDecimal.ZERO)
                        .afterAverageUnitCost(savedInventory.getAverageUnitCost())
                        .beforeIngredientName(null)
                        .afterIngredientName(ingredient.getName())
                        .beforeUnitSymbol(null)
                        .afterUnitSymbol(ingredient.resolveUnitSymbol())
                        .summary("Tao moi nguyen lieu trong inventory")
                        .occurredAt(LocalDateTime.now())
                        .build()
        );

        return toInventoryItemResponse(savedInventory);
    }

    @Transactional
    public InventoryItemResponse updateInventoryItem(Integer inventoryId, UpdateInventoryItemRequest request, CustomUserDetails currentUser) {
        Inventory inventory = getInventoryOrThrow(inventoryId);
        User actor = resolveActor(currentUser.getUserId());
        Ingredient ingredient = inventory.getIngredient();

        // Chụp lại toàn bộ dữ liệu "before" để ghi log audit sau khi update.
        String beforeIngredientName = ingredient.getName();
        String beforeUnitSymbol = ingredient.resolveUnitSymbol();
        BigDecimal beforeQuantity = defaultIfNull(inventory.getQuantity());
        BigDecimal beforeMinStockLevel = defaultIfNull(inventory.getMinStockLevel());
        BigDecimal beforeReorderLevel = defaultIfNull(inventory.getReorderLevel());
        BigDecimal beforeAverageUnitCost = defaultIfNull(inventory.getAverageUnitCost());

        if (StringUtils.hasText(request.ingredientName())) {
            String normalizedIngredientName = request.ingredientName().trim();
            if (ingredientRepository.existsByNameIgnoreCaseAndIdNot(normalizedIngredientName, ingredient.getId())) {
                throw new ConflictException("Ingredient name already exists");
            }
            ingredient.setName(normalizedIngredientName);
        }

        if (request.unitId() != null) {
            MeasurementUnit newUnit = resolveMeasurementUnit(request.unitId());
            Integer currentUnitId = ingredient.getMeasurementUnit() != null ? ingredient.getMeasurementUnit().getId() : null;

            if (!Objects.equals(currentUnitId, newUnit.getId())) {
                // Khong cho doi don vi neu ingredient da duoc dung trong recipe/lich su kho,
                // vi khi do cac so luong cu se bi sai nghia nghiep vu.
                if (recipeRepository.existsByIngredientId(ingredient.getId()) || stockMovementRepository.existsByIngredientId(ingredient.getId())) {
                    throw new ConflictException("Cannot change unit for an ingredient already used in recipe or stock history");
                }
                ingredient.setMeasurementUnit(newUnit);
                ingredient.setLegacyUnit(newUnit.getSymbol());
            }
        }

        // Lưu Ingredient trước vì inventory response và log phía sau đều phụ thuộc dữ liệu ingredient mới.
        ingredientRepository.save(ingredient);

        BigDecimal afterMinStockLevel = request.minStockLevel() != null ? defaultIfNull(request.minStockLevel()) : beforeMinStockLevel;
        BigDecimal afterReorderLevel = request.reorderLevel() != null ? defaultIfNull(request.reorderLevel()) : beforeReorderLevel;
        validateThresholds(afterMinStockLevel, afterReorderLevel);

        if (request.quantity() != null) {
            inventory.setQuantity(defaultIfNull(request.quantity()));
            // Mọi thay đổi quantity kiểu manual update đều được xem là một lần kiểm/điều chỉnh tồn.
            inventory.setLastCountedAt(LocalDateTime.now());
        }
        if (request.minStockLevel() != null) {
            inventory.setMinStockLevel(afterMinStockLevel);
        }
        if (request.reorderLevel() != null) {
            inventory.setReorderLevel(afterReorderLevel);
        }
        if (request.averageUnitCost() != null) {
            inventory.setAverageUnitCost(defaultIfNull(request.averageUnitCost()));
        }

        Inventory savedInventory = inventoryRepository.save(inventory);

        if (savedInventory.getQuantity().compareTo(beforeQuantity) != 0) {
            // Chỉ khi quantity đổi mới cần tạo stock movement.
            createStockMovementForManualChange(
                    savedInventory,
                    actor,
                    beforeQuantity,
                    savedInventory.getQuantity(),
                    request.note(),
                    "Cap nhat ton kho thu cong"
            );
        }

        inventoryActionLogRepository.save(
                InventoryActionLog.builder()
                        .inventoryId(savedInventory.getId())
                        .branchId(savedInventory.getBranch().getId())
                        .branchName(savedInventory.getBranch().getName())
                        .ingredientId(savedInventory.getIngredient().getId())
                        .ingredientName(savedInventory.getIngredient().getName())
                        .unitSymbol(savedInventory.getIngredient().resolveUnitSymbol())
                        .actedBy(actor)
                        .actedByUsername(actor.getUsername())
                        .actedByFullName(actor.getProfile() != null ? actor.getProfile().getFullName() : null)
                        .actionType(InventoryActionType.UPDATED)
                        .beforeQuantity(beforeQuantity)
                        .afterQuantity(defaultIfNull(savedInventory.getQuantity()))
                        .beforeMinStockLevel(beforeMinStockLevel)
                        .afterMinStockLevel(defaultIfNull(savedInventory.getMinStockLevel()))
                        .beforeReorderLevel(beforeReorderLevel)
                        .afterReorderLevel(defaultIfNull(savedInventory.getReorderLevel()))
                        .beforeAverageUnitCost(beforeAverageUnitCost)
                        .afterAverageUnitCost(defaultIfNull(savedInventory.getAverageUnitCost()))
                        .beforeIngredientName(beforeIngredientName)
                        .afterIngredientName(savedInventory.getIngredient().getName())
                        .beforeUnitSymbol(beforeUnitSymbol)
                        .afterUnitSymbol(savedInventory.getIngredient().resolveUnitSymbol())
                        .summary(buildUpdateSummary(
                                beforeIngredientName,
                                savedInventory.getIngredient().getName(),
                                beforeUnitSymbol,
                                savedInventory.getIngredient().resolveUnitSymbol(),
                                beforeQuantity,
                                defaultIfNull(savedInventory.getQuantity()),
                                beforeMinStockLevel,
                                defaultIfNull(savedInventory.getMinStockLevel()),
                                beforeReorderLevel,
                                defaultIfNull(savedInventory.getReorderLevel()),
                                beforeAverageUnitCost,
                                defaultIfNull(savedInventory.getAverageUnitCost())
                        ))
                        .occurredAt(LocalDateTime.now())
                        .build()
        );

        return toInventoryItemResponse(savedInventory);
    }

    @Transactional
    public void deleteInventoryItem(Integer inventoryId, CustomUserDetails currentUser) {
        Inventory inventory = getInventoryOrThrow(inventoryId);
        User actor = resolveActor(currentUser.getUserId());

        if (defaultIfNull(inventory.getQuantity()).compareTo(BigDecimal.ZERO) > 0) {
            // Bắt buộc đưa tồn về 0 trước rồi mới xóa mềm để tránh làm mất dấu hàng đang còn trong kho.
            throw new ConflictException("Cannot delete inventory item while quantity is greater than 0. Adjust stock to 0 first");
        }

        if (recipeRepository.existsByIngredientIdAndBranchId(inventory.getIngredient().getId(), inventory.getBranch().getId())) {
            // Không cho xóa nếu nguyên liệu vẫn đang tham gia recipe của chi nhánh đó.
            throw new ConflictException("Cannot delete inventory item because this ingredient is still used in recipe of the branch");
        }

        inventoryActionLogRepository.save(
                InventoryActionLog.builder()
                        .inventoryId(inventory.getId())
                        .branchId(inventory.getBranch().getId())
                        .branchName(inventory.getBranch().getName())
                        .ingredientId(inventory.getIngredient().getId())
                        .ingredientName(inventory.getIngredient().getName())
                        .unitSymbol(inventory.getIngredient().resolveUnitSymbol())
                        .actedBy(actor)
                        .actedByUsername(actor.getUsername())
                        .actedByFullName(actor.getProfile() != null ? actor.getProfile().getFullName() : null)
                        .actionType(InventoryActionType.SOFT_DELETED)
                        .beforeQuantity(defaultIfNull(inventory.getQuantity()))
                        .afterQuantity(defaultIfNull(inventory.getQuantity()))
                        .beforeMinStockLevel(defaultIfNull(inventory.getMinStockLevel()))
                        .afterMinStockLevel(defaultIfNull(inventory.getMinStockLevel()))
                        .beforeReorderLevel(defaultIfNull(inventory.getReorderLevel()))
                        .afterReorderLevel(defaultIfNull(inventory.getReorderLevel()))
                        .beforeAverageUnitCost(defaultIfNull(inventory.getAverageUnitCost()))
                        .afterAverageUnitCost(defaultIfNull(inventory.getAverageUnitCost()))
                        .beforeIngredientName(inventory.getIngredient().getName())
                        .afterIngredientName(inventory.getIngredient().getName())
                        .beforeUnitSymbol(inventory.getIngredient().resolveUnitSymbol())
                        .afterUnitSymbol(inventory.getIngredient().resolveUnitSymbol())
                        .summary("Xoa mem nguyen lieu khoi inventory")
                        .occurredAt(LocalDateTime.now())
                        .build()
        );

        inventoryRepository.delete(inventory);
    }

    private Inventory getInventoryOrThrow(Integer inventoryId) {
        return inventoryRepository.findDetailById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventory item not found"));
    }

    private void ensureInventoryExists(Integer inventoryId) {
        if (inventoryRepository.findDetailById(inventoryId).isEmpty()) {
            throw new NotFoundException("Inventory item not found");
        }
    }

    private User resolveActor(Integer userId) {
        return userRepository.findEmployeeDetailById(userId)
                .orElseThrow(() -> new NotFoundException("Current user not found"));
    }

    private Branch resolveBranch(Integer branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found"));
    }

    private MeasurementUnit resolveMeasurementUnit(Integer unitId) {
        return measurementUnitRepository.findById(unitId)
                .orElseThrow(() -> new NotFoundException("Measurement unit not found"));
    }

    private Ingredient resolveOrCreateIngredient(String ingredientName, MeasurementUnit unit) {
        String normalizedName = ingredientName.trim();

        // Giu Ingredient lam master dung chung cho recipe/menu de khong pha vo luong bep dang chay on.
        // FE se lay danh sach nguyen lieu qua inventory API, nhung recipe van tham chieu Ingredient.
        return ingredientRepository.findByNameIgnoreCase(normalizedName)
                .map(existingIngredient -> {
                    Integer existingUnitId = existingIngredient.getMeasurementUnit() != null ? existingIngredient.getMeasurementUnit().getId() : null;
                    if (existingUnitId != null && !existingUnitId.equals(unit.getId())) {
                        throw new ConflictException("Ingredient already exists with another unit");
                    }
                    if (existingIngredient.getMeasurementUnit() == null) {
                        existingIngredient.setMeasurementUnit(unit);
                        existingIngredient.setLegacyUnit(unit.getSymbol());
                    }
                    return ingredientRepository.save(existingIngredient);
                })
                .orElseGet(() -> ingredientRepository.save(
                        Ingredient.builder()
                                .name(normalizedName)
                                .measurementUnit(unit)
                                .legacyUnit(unit.getSymbol())
                                .build()
                ));
    }

    private void validateThresholds(BigDecimal minStockLevel, BigDecimal reorderLevel) {
        BigDecimal normalizedMin = defaultIfNull(minStockLevel);
        BigDecimal normalizedReorder = defaultIfNull(reorderLevel);

        if (normalizedReorder.compareTo(normalizedMin) < 0) {
            throw new ConflictException("Reorder level must be greater than or equal to min stock level");
        }
    }

    private void createStockMovementForManualChange(
            Inventory inventory,
            User actor,
            BigDecimal beforeQuantity,
            BigDecimal afterQuantity,
            String note,
            String defaultNote
    ) {
        BigDecimal delta = afterQuantity.subtract(beforeQuantity);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            // Không có thay đổi số lượng thì không cần log stock movement.
            return;
        }

        StockMovementType movementType = delta.compareTo(BigDecimal.ZERO) > 0
                ? StockMovementType.ADJUSTMENT_IN
                : StockMovementType.ADJUSTMENT_OUT;

        BigDecimal unitCost = defaultIfNull(inventory.getAverageUnitCost());

        stockMovementRepository.save(
                StockMovement.builder()
                        .branch(inventory.getBranch())
                        .ingredient(inventory.getIngredient())
                        .createdBy(actor)
                        .movementType(movementType)
                        .quantityChange(delta)
                        .balanceAfter(afterQuantity)
                        .unitCost(unitCost)
                        .totalCost(unitCost.multiply(delta.abs()))
                        .occurredAt(LocalDateTime.now())
                        .note(StringUtils.hasText(note) ? note.trim() : defaultNote)
                        .build()
        );
    }

    private String buildUpdateSummary(
            String beforeIngredientName,
            String afterIngredientName,
            String beforeUnitSymbol,
            String afterUnitSymbol,
            BigDecimal beforeQuantity,
            BigDecimal afterQuantity,
            BigDecimal beforeMinStockLevel,
            BigDecimal afterMinStockLevel,
            BigDecimal beforeReorderLevel,
            BigDecimal afterReorderLevel,
            BigDecimal beforeAverageUnitCost,
            BigDecimal afterAverageUnitCost
    ) {
        List<String> changes = new ArrayList<>();

        if (!Objects.equals(beforeIngredientName, afterIngredientName)) {
            changes.add("doi ten nguyen lieu");
        }
        if (!Objects.equals(beforeUnitSymbol, afterUnitSymbol)) {
            changes.add("doi don vi");
        }
        if (beforeQuantity.compareTo(afterQuantity) != 0) {
            changes.add("cap nhat so luong ton");
        }
        if (beforeMinStockLevel.compareTo(afterMinStockLevel) != 0) {
            changes.add("cap nhat min stock");
        }
        if (beforeReorderLevel.compareTo(afterReorderLevel) != 0) {
            changes.add("cap nhat reorder level");
        }
        if (beforeAverageUnitCost.compareTo(afterAverageUnitCost) != 0) {
            changes.add("cap nhat gia von trung binh");
        }

        return changes.isEmpty()
                ? "Khong co thay doi du lieu"
                : String.join("; ", changes);
    }

    private InventoryItemResponse toInventoryItemResponse(Inventory inventory) {
        BigDecimal currentQuantity = defaultIfNull(inventory.getQuantity());
        BigDecimal minStockLevel = defaultIfNull(inventory.getMinStockLevel());

        boolean outOfStock = currentQuantity.compareTo(BigDecimal.ZERO) == 0;
        boolean lowStock = inventory.getMinStockLevel() != null && currentQuantity.compareTo(minStockLevel) <= 0;

        // alertMessage chỉ sinh khi item đang chạm ngưỡng cảnh báo để FE hiển thị nhanh.
        return new InventoryItemResponse(
                inventory.getId(),
                inventory.getBranch().getId(),
                inventory.getBranch().getName(),
                inventory.getIngredient().getId(),
                inventory.getIngredient().getName(),
                inventory.getIngredient().getMeasurementUnit() != null ? inventory.getIngredient().getMeasurementUnit().getId() : null,
                inventory.getIngredient().resolveUnitName(),
                inventory.getIngredient().resolveUnitSymbol(),
                currentQuantity,
                minStockLevel,
                defaultIfNull(inventory.getReorderLevel()),
                defaultIfNull(inventory.getAverageUnitCost()),
                lowStock,
                outOfStock,
                lowStock ? buildAlertMessage(inventory) : null,
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }

    private InventoryActionLogResponse toInventoryActionLogResponse(InventoryActionLog actionLog) {
        return new InventoryActionLogResponse(
                actionLog.getId(),
                actionLog.getInventoryId(),
                actionLog.getActionType().name(),
                actionLog.getActedByUsername(),
                actionLog.getActedByFullName(),
                actionLog.getBranchName(),
                actionLog.getIngredientName(),
                actionLog.getUnitSymbol(),
                actionLog.getBeforeQuantity(),
                actionLog.getAfterQuantity(),
                actionLog.getBeforeMinStockLevel(),
                actionLog.getAfterMinStockLevel(),
                actionLog.getBeforeReorderLevel(),
                actionLog.getAfterReorderLevel(),
                actionLog.getBeforeAverageUnitCost(),
                actionLog.getAfterAverageUnitCost(),
                actionLog.getBeforeIngredientName(),
                actionLog.getAfterIngredientName(),
                actionLog.getBeforeUnitSymbol(),
                actionLog.getAfterUnitSymbol(),
                actionLog.getSummary(),
                actionLog.getOccurredAt()
        );
    }

    private InventoryAlertResponse toInventoryAlertResponse(Inventory inventory) {
        BigDecimal currentQuantity = defaultIfNull(inventory.getQuantity());
        InventoryAlertLevel level = currentQuantity.compareTo(BigDecimal.ZERO) == 0
                ? InventoryAlertLevel.OUT_OF_STOCK
                : InventoryAlertLevel.BELOW_MIN_STOCK;

        return new InventoryAlertResponse(
                inventory.getId(),
                inventory.getBranch().getId(),
                inventory.getBranch().getName(),
                inventory.getIngredient().getId(),
                inventory.getIngredient().getName(),
                inventory.getIngredient().resolveUnitSymbol(),
                currentQuantity,
                defaultIfNull(inventory.getMinStockLevel()),
                defaultIfNull(inventory.getReorderLevel()),
                level,
                buildAlertMessage(inventory)
        );
    }

    private String buildAlertMessage(Inventory inventory) {
        BigDecimal currentQuantity = defaultIfNull(inventory.getQuantity());
        String unitSymbol = inventory.getIngredient().resolveUnitSymbol();

        if (currentQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return "Nguyen lieu " + inventory.getIngredient().getName()
                    + " tai chi nhanh " + inventory.getBranch().getName()
                    + " da het hang";
        }

        return "Nguyen lieu " + inventory.getIngredient().getName()
                + " tai chi nhanh " + inventory.getBranch().getName()
                + " dang duoi muc ton toi thieu (" + currentQuantity + " " + unitSymbol
                + " / min " + defaultIfNull(inventory.getMinStockLevel()) + " " + unitSymbol + ")";
    }

    private MeasurementUnitResponse toMeasurementUnitResponse(MeasurementUnit unit) {
        return new MeasurementUnitResponse(
                unit.getId(),
                unit.getCode(),
                unit.getName(),
                unit.getSymbol(),
                unit.getDescription()
        );
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
