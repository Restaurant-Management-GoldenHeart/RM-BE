package org.example.goldenheartrestaurant.modules.inventory.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.example.goldenheartrestaurant.modules.inventory.dto.request.CreateInventoryItemRequest;
import org.example.goldenheartrestaurant.modules.inventory.dto.request.InventoryReportGroupBy;
import org.example.goldenheartrestaurant.modules.inventory.dto.request.RestockInventoryItemRequest;
import org.example.goldenheartrestaurant.modules.inventory.dto.request.UpdateInventoryItemRequest;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventoryActionLogResponse;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventoryAlertLevel;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventoryAlertResponse;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventoryItemResponse;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventoryMovementPeriodResponse;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventoryMovementReportResponse;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventorySummaryResponse;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.MeasurementUnitResponse;
import org.example.goldenheartrestaurant.modules.inventory.entity.GoodsReceipt;
import org.example.goldenheartrestaurant.modules.inventory.entity.GoodsReceiptItem;
import org.example.goldenheartrestaurant.modules.inventory.entity.GoodsReceiptStatus;
import org.example.goldenheartrestaurant.modules.inventory.entity.Ingredient;
import org.example.goldenheartrestaurant.modules.inventory.entity.Inventory;
import org.example.goldenheartrestaurant.modules.inventory.entity.InventoryActionLog;
import org.example.goldenheartrestaurant.modules.inventory.entity.InventoryActionType;
import org.example.goldenheartrestaurant.modules.inventory.entity.MeasurementUnit;
import org.example.goldenheartrestaurant.modules.inventory.entity.StockMovement;
import org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType;
import org.example.goldenheartrestaurant.modules.inventory.repository.GoodsReceiptRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.IngredientRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.InventoryActionLogRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.InventoryRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.MeasurementUnitRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.StockMovementRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.projection.InventoryMovementPeriodProjection;
import org.example.goldenheartrestaurant.modules.inventory.repository.projection.InventorySummaryProjection;
import org.example.goldenheartrestaurant.modules.menu.repository.RecipeRepository;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.example.goldenheartrestaurant.modules.restaurant.repository.BranchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InventoryManagementService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_STAFF = "STAFF";
    private static final String ROLE_KITCHEN = "KITCHEN";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_RATE = BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP);

    private final InventoryRepository inventoryRepository;
    private final IngredientRepository ingredientRepository;
    private final MeasurementUnitRepository measurementUnitRepository;
    private final InventoryActionLogRepository inventoryActionLogRepository;
    private final StockMovementRepository stockMovementRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
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

    @Transactional(readOnly = true)
    public InventorySummaryResponse getInventorySummary(Integer branchId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF, ROLE_KITCHEN);
        Integer scopedBranchId = resolveReadableBranchId(branchId, currentUser);
        InventorySummaryProjection projection = inventoryRepository.summarizeCurrentStateByBranch(scopedBranchId);
        return new InventorySummaryResponse(scopedBranchId, resolveBranchName(scopedBranchId), projection.getTotalItems(),
                defaultIfNull(projection.getTotalQuantity()), defaultIfNull(projection.getTotalInventoryValue()),
                defaultIfNull(projection.getLowStockCount()), defaultIfNull(projection.getOutOfStockCount()), LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public InventoryMovementReportResponse getInventoryMovementReport(Integer branchId, LocalDate fromDate, LocalDate toDate,
                                                                     InventoryReportGroupBy groupBy, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF, ROLE_KITCHEN);
        Integer scopedBranchId = resolveReadableBranchId(branchId, currentUser);
        InventoryReportGroupBy effectiveGroupBy = groupBy != null ? groupBy : InventoryReportGroupBy.DAY;
        LocalDate effectiveFromDate = fromDate;
        LocalDate effectiveToDate = toDate;
        if (effectiveFromDate == null && effectiveToDate == null) {
            effectiveToDate = LocalDate.now();
            effectiveFromDate = effectiveToDate.withDayOfMonth(1);
        } else if (effectiveFromDate == null) {
            effectiveFromDate = effectiveToDate;
        } else if (effectiveToDate == null) {
            effectiveToDate = effectiveFromDate;
        }
        if (effectiveFromDate.isAfter(effectiveToDate)) {
            throw new ConflictException("fromDate must be before or equal to toDate");
        }
        LocalDateTime fromDateTime = effectiveFromDate.atStartOfDay();
        LocalDateTime toDateTime = effectiveToDate.plusDays(1).atStartOfDay();
        List<InventoryMovementPeriodProjection> projections = effectiveGroupBy == InventoryReportGroupBy.MONTH
                ? stockMovementRepository.summarizeMovementByMonth(scopedBranchId, fromDateTime, toDateTime)
                : stockMovementRepository.summarizeMovementByDay(scopedBranchId, fromDateTime, toDateTime);
        List<InventoryMovementPeriodResponse> periods = projections.stream().map(this::toInventoryMovementPeriodResponse).toList();
        BigDecimal totalReceiptValue = sumPeriods(periods, InventoryMovementPeriodResponse::receiptValue);
        BigDecimal totalSaleValue = sumPeriods(periods, InventoryMovementPeriodResponse::saleValue);
        BigDecimal totalWasteValue = sumPeriods(periods, InventoryMovementPeriodResponse::wasteValue);
        BigDecimal totalAdjustmentInValue = sumPeriods(periods, InventoryMovementPeriodResponse::adjustmentInValue);
        BigDecimal totalAdjustmentOutValue = sumPeriods(periods, InventoryMovementPeriodResponse::adjustmentOutValue);
        BigDecimal totalStocktakeInValue = sumPeriods(periods, InventoryMovementPeriodResponse::stocktakeInValue);
        BigDecimal totalStocktakeOutValue = sumPeriods(periods, InventoryMovementPeriodResponse::stocktakeOutValue);
        BigDecimal totalReturnOutValue = sumPeriods(periods, InventoryMovementPeriodResponse::returnOutValue);
        BigDecimal totalInValue = sumValues(totalReceiptValue, totalAdjustmentInValue, totalStocktakeInValue);
        BigDecimal totalOutValue = sumValues(totalSaleValue, totalWasteValue, totalAdjustmentOutValue, totalStocktakeOutValue, totalReturnOutValue);
        return new InventoryMovementReportResponse(scopedBranchId, resolveBranchName(scopedBranchId), effectiveGroupBy.name(),
                effectiveFromDate, effectiveToDate, totalReceiptValue, totalSaleValue, totalWasteValue,
                totalAdjustmentInValue, totalAdjustmentOutValue, totalStocktakeInValue, totalStocktakeOutValue,
                totalReturnOutValue, totalInValue, totalOutValue, totalInValue.subtract(totalOutValue), periods, LocalDateTime.now());
    }

    @Transactional
    public InventoryItemResponse createInventoryItem(CreateInventoryItemRequest request, CustomUserDetails currentUser) {
        validateThresholds(request.minStockLevel(), request.reorderLevel());
        Branch branch = resolveBranch(request.branchId());
        MeasurementUnit baseUnit = resolveMeasurementUnit(request.unitId());
        User actor = resolveActor(currentUser.getUserId());
        Ingredient ingredient = resolveOrCreateIngredient(branch, request.ingredientName(), baseUnit);
        if (inventoryRepository.findByBranchIdAndIngredientId(branch.getId(), ingredient.getId()).isPresent()) {
            throw new ConflictException("Inventory item already exists for this branch and ingredient");
        }
        ReceiptLine receiptLine = buildReceiptLine(ingredient, baseUnit, request.quantity(), request.averageUnitCost(),
                request.purchaseQuantity(), request.purchaseUnitId(), request.purchaseToBaseRate(), request.purchaseUnitCost());
        saveDefaultPurchaseUnitIfNeeded(ingredient, receiptLine, request.saveAsDefaultPurchaseUnit());
        BigDecimal initialQuantity = receiptLine.convertedQuantity();
        BigDecimal initialAverageCost = initialQuantity.compareTo(BigDecimal.ZERO) > 0 ? receiptLine.baseUnitCost() : money(request.averageUnitCost());
        LocalDateTime now = LocalDateTime.now();
        Inventory inventory = Inventory.builder().branch(branch).ingredient(ingredient).quantity(initialQuantity)
                .minStockLevel(defaultIfNull(request.minStockLevel())).reorderLevel(defaultIfNull(request.reorderLevel()))
                .averageUnitCost(defaultIfNull(initialAverageCost)).lastReceiptAt(initialQuantity.compareTo(BigDecimal.ZERO) > 0 ? now : null).build();
        Inventory savedInventory = inventoryRepository.save(inventory);
        if (initialQuantity.compareTo(BigDecimal.ZERO) > 0) {
            createReceiptMovement(branch, savedInventory, receiptLine, actor, LocalDate.now(), null, null, null,
                    request.note(), "Tao moi nguyen lieu trong ton kho", now);
        }
        writeInventoryActionLog(savedInventory, actor, InventoryActionType.CREATED, BigDecimal.ZERO, savedInventory.getQuantity(),
                BigDecimal.ZERO, savedInventory.getMinStockLevel(), BigDecimal.ZERO, savedInventory.getReorderLevel(),
                BigDecimal.ZERO, savedInventory.getAverageUnitCost(), null, savedInventory.getIngredient().getName(),
                null, savedInventory.getIngredient().resolveUnitSymbol(), "Tao moi nguyen lieu trong inventory");
        return toInventoryItemResponse(savedInventory);
    }

    @Transactional
    public InventoryItemResponse restockInventoryItem(Integer inventoryId, RestockInventoryItemRequest request, CustomUserDetails currentUser) {
        Inventory inventory = inventoryRepository.findDetailForUpdateById(inventoryId).orElseThrow(() -> new NotFoundException("Inventory item not found"));
        User actor = resolveActor(currentUser.getUserId());
        Ingredient ingredient = inventory.getIngredient();
        MeasurementUnit baseUnit = ingredient.getMeasurementUnit();
        if (baseUnit == null) throw new ConflictException("Inventory item has no base unit");
        LocalDate receiptDate = request.receiptDate() != null ? request.receiptDate() : LocalDate.now();
        if (receiptDate.isAfter(LocalDate.now())) throw new ConflictException("Receipt date must not be in the future");
        if (request.expiryDate() != null && request.expiryDate().isBefore(receiptDate)) throw new ConflictException("Expiry date must not be before receipt date");
        ReceiptLine receiptLine = buildReceiptLine(ingredient, baseUnit, null, null, request.purchaseQuantity(),
                request.purchaseUnitId(), request.purchaseToBaseRate(), request.purchaseUnitCost());
        saveDefaultPurchaseUnitIfNeeded(ingredient, receiptLine, request.saveAsDefaultPurchaseUnit());
        BigDecimal beforeQuantity = defaultIfNull(inventory.getQuantity());
        BigDecimal beforeMinStock = defaultIfNull(inventory.getMinStockLevel());
        BigDecimal beforeReorder = defaultIfNull(inventory.getReorderLevel());
        BigDecimal beforeAverageCost = defaultIfNull(inventory.getAverageUnitCost());
        String beforeIngredientName = ingredient.getName();
        String beforeUnitSymbol = ingredient.resolveUnitSymbol();
        BigDecimal afterQuantity = quantity(beforeQuantity.add(receiptLine.convertedQuantity()));
        BigDecimal afterAverageCost = calculateAverageCost(beforeQuantity, beforeAverageCost, receiptLine.convertedQuantity(), receiptLine.baseUnitCost());
        LocalDateTime occurredAt = LocalDateTime.now();
        inventory.setQuantity(afterQuantity);
        inventory.setAverageUnitCost(afterAverageCost);
        inventory.setLastReceiptAt(occurredAt);
        Inventory savedInventory = inventoryRepository.save(inventory);
        GoodsReceipt receipt = createReceiptMovement(inventory.getBranch(), savedInventory, receiptLine, actor, receiptDate,
                cleanText(request.invoiceNumber()), cleanText(request.batchNumber()), request.expiryDate(), cleanText(request.note()),
                "Nhap hang thu cong", occurredAt);
        writeInventoryActionLog(savedInventory, actor, InventoryActionType.UPDATED, beforeQuantity, afterQuantity,
                beforeMinStock, beforeMinStock, beforeReorder, beforeReorder, beforeAverageCost, afterAverageCost,
                beforeIngredientName, ingredient.getName(), beforeUnitSymbol, ingredient.resolveUnitSymbol(),
                "Nhap hang phieu " + receipt.getReceiptCode() + ": +" + receiptLine.convertedQuantity().stripTrailingZeros().toPlainString()
                        + " " + ingredient.resolveUnitSymbol());
        return toInventoryItemResponse(savedInventory);
    }

    @Transactional
    public InventoryItemResponse updateInventoryItem(Integer inventoryId, UpdateInventoryItemRequest request, CustomUserDetails currentUser) {
        Inventory inventory = getInventoryOrThrow(inventoryId);
        User actor = resolveActor(currentUser.getUserId());
        Ingredient ingredient = inventory.getIngredient();
        String beforeIngredientName = ingredient.getName();
        String beforeUnitSymbol = ingredient.resolveUnitSymbol();
        BigDecimal beforeQuantity = defaultIfNull(inventory.getQuantity());
        BigDecimal beforeMinStockLevel = defaultIfNull(inventory.getMinStockLevel());
        BigDecimal beforeReorderLevel = defaultIfNull(inventory.getReorderLevel());
        BigDecimal beforeAverageUnitCost = defaultIfNull(inventory.getAverageUnitCost());
        if (StringUtils.hasText(request.ingredientName())) {
            String normalizedIngredientName = request.ingredientName().trim();
            Integer branchId = inventory.getBranch().getId();
            if (ingredientRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(branchId, normalizedIngredientName, ingredient.getId())) {
                throw new ConflictException("Ingredient name already exists");
            }
            ingredient.setName(normalizedIngredientName);
        }
        if (request.unitId() != null) {
            MeasurementUnit newUnit = resolveMeasurementUnit(request.unitId());
            Integer currentUnitId = ingredient.getMeasurementUnit() != null ? ingredient.getMeasurementUnit().getId() : null;
            if (!Objects.equals(currentUnitId, newUnit.getId())) {
                if (recipeRepository.existsByIngredientId(ingredient.getId()) || stockMovementRepository.existsByIngredientId(ingredient.getId())) {
                    throw new ConflictException("Cannot change unit for an ingredient already used in recipe or stock history");
                }
                ingredient.setMeasurementUnit(newUnit);
                ingredient.setLegacyUnit(newUnit.getSymbol());
            }
        }
        ingredientRepository.save(ingredient);
        BigDecimal afterMinStockLevel = request.minStockLevel() != null ? defaultIfNull(request.minStockLevel()) : beforeMinStockLevel;
        BigDecimal afterReorderLevel = request.reorderLevel() != null ? defaultIfNull(request.reorderLevel()) : beforeReorderLevel;
        validateThresholds(afterMinStockLevel, afterReorderLevel);
        if (request.quantity() != null) {
            inventory.setQuantity(defaultIfNull(request.quantity()));
            inventory.setLastCountedAt(LocalDateTime.now());
        }
        if (request.minStockLevel() != null) inventory.setMinStockLevel(afterMinStockLevel);
        if (request.reorderLevel() != null) inventory.setReorderLevel(afterReorderLevel);
        if (request.averageUnitCost() != null) inventory.setAverageUnitCost(defaultIfNull(request.averageUnitCost()));
        Inventory savedInventory = inventoryRepository.save(inventory);
        if (savedInventory.getQuantity().compareTo(beforeQuantity) != 0) {
            createStockMovementForManualChange(savedInventory, actor, beforeQuantity, savedInventory.getQuantity(), request.note(), "Cap nhat ton kho thu cong");
        }
        writeInventoryActionLog(savedInventory, actor, InventoryActionType.UPDATED, beforeQuantity, defaultIfNull(savedInventory.getQuantity()),
                beforeMinStockLevel, defaultIfNull(savedInventory.getMinStockLevel()), beforeReorderLevel, defaultIfNull(savedInventory.getReorderLevel()),
                beforeAverageUnitCost, defaultIfNull(savedInventory.getAverageUnitCost()), beforeIngredientName, savedInventory.getIngredient().getName(),
                beforeUnitSymbol, savedInventory.getIngredient().resolveUnitSymbol(), buildUpdateSummary(beforeIngredientName,
                        savedInventory.getIngredient().getName(), beforeUnitSymbol, savedInventory.getIngredient().resolveUnitSymbol(),
                        beforeQuantity, defaultIfNull(savedInventory.getQuantity()), beforeMinStockLevel, defaultIfNull(savedInventory.getMinStockLevel()),
                        beforeReorderLevel, defaultIfNull(savedInventory.getReorderLevel()), beforeAverageUnitCost,
                        defaultIfNull(savedInventory.getAverageUnitCost())));
        return toInventoryItemResponse(savedInventory);
    }

    @Transactional
    public void deleteInventoryItem(Integer inventoryId, CustomUserDetails currentUser) {
        Inventory inventory = getInventoryOrThrow(inventoryId);
        User actor = resolveActor(currentUser.getUserId());
        if (defaultIfNull(inventory.getQuantity()).compareTo(BigDecimal.ZERO) > 0) {
            throw new ConflictException("Cannot delete inventory item while quantity is greater than 0. Adjust stock to 0 first");
        }
        if (recipeRepository.existsByIngredientIdAndBranchId(inventory.getIngredient().getId(), inventory.getBranch().getId())) {
            throw new ConflictException("Cannot delete inventory item because this ingredient is still used in recipe of the branch");
        }
        writeInventoryActionLog(inventory, actor, InventoryActionType.SOFT_DELETED, defaultIfNull(inventory.getQuantity()),
                defaultIfNull(inventory.getQuantity()), defaultIfNull(inventory.getMinStockLevel()), defaultIfNull(inventory.getMinStockLevel()),
                defaultIfNull(inventory.getReorderLevel()), defaultIfNull(inventory.getReorderLevel()), defaultIfNull(inventory.getAverageUnitCost()),
                defaultIfNull(inventory.getAverageUnitCost()), inventory.getIngredient().getName(), inventory.getIngredient().getName(),
                inventory.getIngredient().resolveUnitSymbol(), inventory.getIngredient().resolveUnitSymbol(), "Xoa mem nguyen lieu khoi inventory");
        inventoryRepository.delete(inventory);
    }

    private ReceiptLine buildReceiptLine(Ingredient ingredient, MeasurementUnit baseUnit, BigDecimal fallbackBaseQuantity,
                                         BigDecimal fallbackBaseUnitCost, BigDecimal purchaseQuantity, Integer purchaseUnitId,
                                         BigDecimal purchaseToBaseRate, BigDecimal purchaseUnitCost) {
        boolean hasPurchaseData = purchaseQuantity != null || purchaseUnitId != null || purchaseToBaseRate != null || purchaseUnitCost != null;
        if (!hasPurchaseData) {
            BigDecimal baseQuantity = quantity(fallbackBaseQuantity);
            BigDecimal baseUnitCost = money(fallbackBaseUnitCost);
            return new ReceiptLine(baseQuantity, baseUnit, baseUnit, ONE_RATE, baseQuantity, baseUnitCost,
                    money(baseQuantity.multiply(baseUnitCost)), false);
        }
        if (purchaseQuantity == null || purchaseQuantity.compareTo(BigDecimal.ZERO) <= 0) throw new ConflictException("Purchase quantity must be greater than 0");
        if (purchaseUnitId == null) throw new ConflictException("Purchase unit is required");
        if (purchaseUnitCost == null || purchaseUnitCost.compareTo(BigDecimal.ZERO) < 0) throw new ConflictException("Purchase unit cost must be greater than or equal to 0");
        MeasurementUnit purchaseUnit = resolveMeasurementUnit(purchaseUnitId);
        BigDecimal rate = resolveConversionRate(ingredient, baseUnit, purchaseUnit, purchaseToBaseRate);
        BigDecimal normalizedPurchaseQuantity = quantity(purchaseQuantity);
        BigDecimal convertedQuantity = quantity(normalizedPurchaseQuantity.multiply(rate));
        if (convertedQuantity.compareTo(BigDecimal.ZERO) <= 0) throw new ConflictException("Converted quantity must be greater than 0");
        BigDecimal normalizedPurchaseUnitCost = money(purchaseUnitCost);
        BigDecimal lineTotal = money(normalizedPurchaseQuantity.multiply(normalizedPurchaseUnitCost));
        BigDecimal baseUnitCost = money(lineTotal.divide(convertedQuantity, 2, RoundingMode.HALF_UP));
        return new ReceiptLine(normalizedPurchaseQuantity, purchaseUnit, baseUnit, rate, convertedQuantity, baseUnitCost, lineTotal, true);
    }

    private BigDecimal resolveConversionRate(Ingredient ingredient, MeasurementUnit baseUnit, MeasurementUnit purchaseUnit, BigDecimal requestRate) {
        if (requestRate != null) {
            if (requestRate.compareTo(BigDecimal.ZERO) <= 0) throw new ConflictException("Purchase conversion rate must be greater than 0");
            return rate(requestRate);
        }
        Integer purchaseUnitId = purchaseUnit != null ? purchaseUnit.getId() : null;
        Integer baseUnitId = baseUnit != null ? baseUnit.getId() : null;
        if (Objects.equals(purchaseUnitId, baseUnitId)) return ONE_RATE;
        Integer defaultPurchaseUnitId = ingredient.getDefaultPurchaseUnit() != null ? ingredient.getDefaultPurchaseUnit().getId() : null;
        if (Objects.equals(defaultPurchaseUnitId, purchaseUnitId) && ingredient.getDefaultPurchaseToBaseRate() != null) {
            return rate(ingredient.getDefaultPurchaseToBaseRate());
        }
        throw new ConflictException("Purchase conversion rate is required for this purchase unit");
    }

    private void saveDefaultPurchaseUnitIfNeeded(Ingredient ingredient, ReceiptLine receiptLine, Boolean requestFlag) {
        boolean shouldSave = Boolean.TRUE.equals(requestFlag) || (requestFlag == null && receiptLine.fromPurchaseUnit());
        if (!shouldSave || !receiptLine.fromPurchaseUnit()) return;
        ingredient.setDefaultPurchaseUnit(receiptLine.purchaseUnit());
        ingredient.setDefaultPurchaseToBaseRate(receiptLine.conversionRate());
        ingredientRepository.save(ingredient);
    }

    private GoodsReceipt createReceiptMovement(Branch branch, Inventory inventory, ReceiptLine receiptLine, User actor,
                                               LocalDate receiptDate, String invoiceNumber, String batchNumber,
                                               LocalDate expiryDate, String note, String defaultNote, LocalDateTime occurredAt) {
        String receiptCode = generateReceiptCode(occurredAt, actor.getId());
        GoodsReceipt receipt = goodsReceiptRepository.save(GoodsReceipt.builder().receiptCode(receiptCode).branch(branch)
                .receivedBy(actor).receiptDate(receiptDate).invoiceNumber(invoiceNumber).subtotalAmount(receiptLine.lineTotal())
                .taxAmount(ZERO).totalAmount(receiptLine.lineTotal()).status(GoodsReceiptStatus.RECEIVED)
                .note(StringUtils.hasText(note) ? note : defaultNote).createdAt(occurredAt).items(new ArrayList<>()).build());
        GoodsReceiptItem item = GoodsReceiptItem.builder().goodsReceipt(receipt).ingredient(inventory.getIngredient())
                .quantity(receiptLine.convertedQuantity()).unitCost(receiptLine.baseUnitCost()).lineTotal(receiptLine.lineTotal())
                .purchaseQuantity(receiptLine.purchaseQuantity()).purchaseUnit(receiptLine.purchaseUnit())
                .conversionRate(receiptLine.conversionRate()).convertedQuantity(receiptLine.convertedQuantity())
                .baseUnit(receiptLine.baseUnit()).batchNumber(batchNumber).expiryDate(expiryDate).note(note).build();
        receipt.getItems().add(item);
        GoodsReceipt savedReceipt = goodsReceiptRepository.save(receipt);
        stockMovementRepository.save(StockMovement.builder().branch(branch).ingredient(inventory.getIngredient()).goodsReceipt(savedReceipt)
                .createdBy(actor).movementType(StockMovementType.RECEIPT_IN).quantityChange(receiptLine.convertedQuantity())
                .balanceAfter(defaultIfNull(inventory.getQuantity())).unitCost(receiptLine.baseUnitCost()).totalCost(receiptLine.lineTotal())
                .occurredAt(occurredAt).note(buildReceiptMovementNote(savedReceipt.getReceiptCode(), receiptLine, note, defaultNote)).build());
        return savedReceipt;
    }

    private String buildReceiptMovementNote(String receiptCode, ReceiptLine receiptLine, String note, String defaultNote) {
        String conversion = receiptLine.purchaseQuantity().stripTrailingZeros().toPlainString() + " " + receiptLine.purchaseUnit().getSymbol()
                + " -> " + receiptLine.convertedQuantity().stripTrailingZeros().toPlainString() + " " + receiptLine.baseUnit().getSymbol();
        String prefix = defaultNote + " " + receiptCode + " (" + conversion + ")";
        return StringUtils.hasText(note) ? prefix + " - " + note : prefix;
    }

    private Inventory getInventoryOrThrow(Integer inventoryId) {
        return inventoryRepository.findDetailById(inventoryId).orElseThrow(() -> new NotFoundException("Inventory item not found"));
    }

    private void ensureInventoryExists(Integer inventoryId) {
        if (inventoryRepository.findDetailById(inventoryId).isEmpty()) throw new NotFoundException("Inventory item not found");
    }

    private User resolveActor(Integer userId) {
        return userRepository.findEmployeeDetailById(userId).orElseThrow(() -> new NotFoundException("Current user not found"));
    }

    private Integer resolveReadableBranchId(Integer branchId, CustomUserDetails currentUser) {
        if (hasRole(currentUser, ROLE_ADMIN) || hasRole(currentUser, ROLE_MANAGER)) return branchId;
        User currentUserEntity = resolveActor(currentUser.getUserId());
        if (currentUserEntity.getProfile() == null || currentUserEntity.getProfile().getBranch() == null) {
            throw new ForbiddenException("Your account is not assigned to any branch");
        }
        Integer ownBranchId = currentUserEntity.getProfile().getBranch().getId();
        if (branchId != null && !branchId.equals(ownBranchId)) throw new ForbiddenException("You do not have permission to view another branch inventory");
        return ownBranchId;
    }

    private Branch resolveBranch(Integer branchId) {
        return branchRepository.findById(branchId).orElseThrow(() -> new NotFoundException("Branch not found"));
    }

    private String resolveBranchName(Integer branchId) {
        if (branchId == null) return null;
        return branchRepository.findById(branchId).map(Branch::getName).orElseThrow(() -> new NotFoundException("Branch not found"));
    }

    private MeasurementUnit resolveMeasurementUnit(Integer unitId) {
        return measurementUnitRepository.findById(unitId).orElseThrow(() -> new NotFoundException("Measurement unit not found"));
    }

    private Ingredient resolveOrCreateIngredient(Branch branch, String ingredientName, MeasurementUnit unit) {
        String normalizedName = ingredientName.trim();
        return ingredientRepository.findByBranchIdAndNameIgnoreCase(branch.getId(), normalizedName).map(existingIngredient -> {
            Integer existingUnitId = existingIngredient.getMeasurementUnit() != null ? existingIngredient.getMeasurementUnit().getId() : null;
            if (existingUnitId != null && !existingUnitId.equals(unit.getId())) throw new ConflictException("Ingredient already exists with another unit");
            if (existingIngredient.getMeasurementUnit() == null) {
                existingIngredient.setMeasurementUnit(unit);
                existingIngredient.setLegacyUnit(unit.getSymbol());
            }
            return ingredientRepository.save(existingIngredient);
        }).orElseGet(() -> ingredientRepository.save(Ingredient.builder().name(normalizedName).branch(branch)
                .measurementUnit(unit).legacyUnit(unit.getSymbol()).build()));
    }

    private void validateThresholds(BigDecimal minStockLevel, BigDecimal reorderLevel) {
        BigDecimal normalizedMin = defaultIfNull(minStockLevel);
        BigDecimal normalizedReorder = defaultIfNull(reorderLevel);
        if (normalizedReorder.compareTo(normalizedMin) < 0) throw new ConflictException("Reorder level must be greater than or equal to min stock level");
    }

    private void createStockMovementForManualChange(Inventory inventory, User actor, BigDecimal beforeQuantity,
                                                    BigDecimal afterQuantity, String note, String defaultNote) {
        BigDecimal delta = afterQuantity.subtract(beforeQuantity);
        if (delta.compareTo(BigDecimal.ZERO) == 0) return;
        StockMovementType movementType = delta.compareTo(BigDecimal.ZERO) > 0 ? StockMovementType.ADJUSTMENT_IN : StockMovementType.ADJUSTMENT_OUT;
        BigDecimal unitCost = defaultIfNull(inventory.getAverageUnitCost());
        stockMovementRepository.save(StockMovement.builder().branch(inventory.getBranch()).ingredient(inventory.getIngredient())
                .createdBy(actor).movementType(movementType).quantityChange(delta).balanceAfter(afterQuantity).unitCost(unitCost)
                .totalCost(unitCost.multiply(delta.abs())).occurredAt(LocalDateTime.now())
                .note(StringUtils.hasText(note) ? note.trim() : defaultNote).build());
    }

    private void writeInventoryActionLog(Inventory inventory, User actor, InventoryActionType actionType,
                                         BigDecimal beforeQuantity, BigDecimal afterQuantity, BigDecimal beforeMinStockLevel,
                                         BigDecimal afterMinStockLevel, BigDecimal beforeReorderLevel, BigDecimal afterReorderLevel,
                                         BigDecimal beforeAverageUnitCost, BigDecimal afterAverageUnitCost, String beforeIngredientName,
                                         String afterIngredientName, String beforeUnitSymbol, String afterUnitSymbol, String summary) {
        inventoryActionLogRepository.save(InventoryActionLog.builder().inventoryId(inventory.getId()).branchId(inventory.getBranch().getId())
                .branchName(inventory.getBranch().getName()).ingredientId(inventory.getIngredient().getId())
                .ingredientName(inventory.getIngredient().getName()).unitSymbol(inventory.getIngredient().resolveUnitSymbol())
                .actedBy(actor).actedByUsername(actor.getUsername()).actedByFullName(actor.getProfile() != null ? actor.getProfile().getFullName() : null)
                .actionType(actionType).beforeQuantity(defaultIfNull(beforeQuantity)).afterQuantity(defaultIfNull(afterQuantity))
                .beforeMinStockLevel(defaultIfNull(beforeMinStockLevel)).afterMinStockLevel(defaultIfNull(afterMinStockLevel))
                .beforeReorderLevel(defaultIfNull(beforeReorderLevel)).afterReorderLevel(defaultIfNull(afterReorderLevel))
                .beforeAverageUnitCost(defaultIfNull(beforeAverageUnitCost)).afterAverageUnitCost(defaultIfNull(afterAverageUnitCost))
                .beforeIngredientName(beforeIngredientName).afterIngredientName(afterIngredientName).beforeUnitSymbol(beforeUnitSymbol)
                .afterUnitSymbol(afterUnitSymbol).summary(summary).occurredAt(LocalDateTime.now()).build());
    }

    private String buildUpdateSummary(String beforeIngredientName, String afterIngredientName, String beforeUnitSymbol,
                                      String afterUnitSymbol, BigDecimal beforeQuantity, BigDecimal afterQuantity,
                                      BigDecimal beforeMinStockLevel, BigDecimal afterMinStockLevel, BigDecimal beforeReorderLevel,
                                      BigDecimal afterReorderLevel, BigDecimal beforeAverageUnitCost, BigDecimal afterAverageUnitCost) {
        List<String> changes = new ArrayList<>();
        if (!Objects.equals(beforeIngredientName, afterIngredientName)) changes.add("doi ten nguyen lieu");
        if (!Objects.equals(beforeUnitSymbol, afterUnitSymbol)) changes.add("doi don vi");
        if (beforeQuantity.compareTo(afterQuantity) != 0) changes.add("cap nhat so luong ton");
        if (beforeMinStockLevel.compareTo(afterMinStockLevel) != 0) changes.add("cap nhat min stock");
        if (beforeReorderLevel.compareTo(afterReorderLevel) != 0) changes.add("cap nhat reorder level");
        if (beforeAverageUnitCost.compareTo(afterAverageUnitCost) != 0) changes.add("cap nhat gia von trung binh");
        return changes.isEmpty() ? "Khong co thay doi du lieu" : String.join("; ", changes);
    }

    private InventoryItemResponse toInventoryItemResponse(Inventory inventory) {
        BigDecimal currentQuantity = defaultIfNull(inventory.getQuantity());
        BigDecimal minStockLevel = defaultIfNull(inventory.getMinStockLevel());
        boolean outOfStock = currentQuantity.compareTo(BigDecimal.ZERO) == 0;
        boolean lowStock = inventory.getMinStockLevel() != null && currentQuantity.compareTo(minStockLevel) <= 0;
        Ingredient ingredient = inventory.getIngredient();
        MeasurementUnit defaultPurchaseUnit = ingredient.getDefaultPurchaseUnit();
        return new InventoryItemResponse(inventory.getId(), inventory.getBranch().getId(), inventory.getBranch().getName(),
                ingredient.getId(), ingredient.getName(), ingredient.getMeasurementUnit() != null ? ingredient.getMeasurementUnit().getId() : null,
                ingredient.resolveUnitName(), ingredient.resolveUnitSymbol(), currentQuantity, minStockLevel,
                defaultIfNull(inventory.getReorderLevel()), defaultIfNull(inventory.getAverageUnitCost()), lowStock, outOfStock,
                lowStock ? buildAlertMessage(inventory) : null, inventory.getCreatedAt(), inventory.getUpdatedAt(),
                defaultPurchaseUnit != null ? defaultPurchaseUnit.getId() : null,
                defaultPurchaseUnit != null ? defaultPurchaseUnit.getName() : null,
                defaultPurchaseUnit != null ? defaultPurchaseUnit.getSymbol() : null,
                ingredient.getDefaultPurchaseToBaseRate());
    }

    private InventoryActionLogResponse toInventoryActionLogResponse(InventoryActionLog actionLog) {
        return new InventoryActionLogResponse(actionLog.getId(), actionLog.getInventoryId(), actionLog.getActionType().name(),
                actionLog.getActedByUsername(), actionLog.getActedByFullName(), actionLog.getBranchName(), actionLog.getIngredientName(),
                actionLog.getUnitSymbol(), actionLog.getBeforeQuantity(), actionLog.getAfterQuantity(), actionLog.getBeforeMinStockLevel(),
                actionLog.getAfterMinStockLevel(), actionLog.getBeforeReorderLevel(), actionLog.getAfterReorderLevel(),
                actionLog.getBeforeAverageUnitCost(), actionLog.getAfterAverageUnitCost(), actionLog.getBeforeIngredientName(),
                actionLog.getAfterIngredientName(), actionLog.getBeforeUnitSymbol(), actionLog.getAfterUnitSymbol(),
                actionLog.getSummary(), actionLog.getOccurredAt());
    }

    private InventoryAlertResponse toInventoryAlertResponse(Inventory inventory) {
        BigDecimal currentQuantity = defaultIfNull(inventory.getQuantity());
        InventoryAlertLevel level = currentQuantity.compareTo(BigDecimal.ZERO) == 0 ? InventoryAlertLevel.OUT_OF_STOCK : InventoryAlertLevel.BELOW_MIN_STOCK;
        return new InventoryAlertResponse(inventory.getId(), inventory.getBranch().getId(), inventory.getBranch().getName(),
                inventory.getIngredient().getId(), inventory.getIngredient().getName(), inventory.getIngredient().resolveUnitSymbol(),
                currentQuantity, defaultIfNull(inventory.getMinStockLevel()), defaultIfNull(inventory.getReorderLevel()), level, buildAlertMessage(inventory));
    }

    private String buildAlertMessage(Inventory inventory) {
        BigDecimal currentQuantity = defaultIfNull(inventory.getQuantity());
        String unitSymbol = inventory.getIngredient().resolveUnitSymbol();
        if (currentQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return "Nguyen lieu " + inventory.getIngredient().getName() + " tai chi nhanh " + inventory.getBranch().getName() + " da het hang";
        }
        return "Nguyen lieu " + inventory.getIngredient().getName() + " tai chi nhanh " + inventory.getBranch().getName()
                + " dang duoi muc ton toi thieu (" + currentQuantity + " " + unitSymbol + " / min "
                + defaultIfNull(inventory.getMinStockLevel()) + " " + unitSymbol + ")";
    }

    private MeasurementUnitResponse toMeasurementUnitResponse(MeasurementUnit unit) {
        return new MeasurementUnitResponse(unit.getId(), unit.getCode(), unit.getName(), unit.getSymbol(), unit.getDescription());
    }

    private InventoryMovementPeriodResponse toInventoryMovementPeriodResponse(InventoryMovementPeriodProjection projection) {
        BigDecimal receiptValue = defaultIfNull(projection.getReceiptValue());
        BigDecimal saleValue = defaultIfNull(projection.getSaleValue());
        BigDecimal wasteValue = defaultIfNull(projection.getWasteValue());
        BigDecimal adjustmentInValue = defaultIfNull(projection.getAdjustmentInValue());
        BigDecimal adjustmentOutValue = defaultIfNull(projection.getAdjustmentOutValue());
        BigDecimal stocktakeInValue = defaultIfNull(projection.getStocktakeInValue());
        BigDecimal stocktakeOutValue = defaultIfNull(projection.getStocktakeOutValue());
        BigDecimal returnOutValue = defaultIfNull(projection.getReturnOutValue());
        BigDecimal totalInValue = sumValues(receiptValue, adjustmentInValue, stocktakeInValue);
        BigDecimal totalOutValue = sumValues(saleValue, wasteValue, adjustmentOutValue, stocktakeOutValue, returnOutValue);
        return new InventoryMovementPeriodResponse(projection.getPeriodKey(), receiptValue, saleValue, wasteValue, adjustmentInValue,
                adjustmentOutValue, stocktakeInValue, stocktakeOutValue, returnOutValue, totalInValue, totalOutValue,
                totalInValue.subtract(totalOutValue));
    }

    private String normalizeKeyword(String keyword) { return StringUtils.hasText(keyword) ? keyword.trim() : null; }
    private String cleanText(String value) { return StringUtils.hasText(value) ? value.trim().replaceAll("\\s+", " ") : null; }

    private String generateReceiptCode(LocalDateTime now, Integer actorId) {
        String baseCode = "GR" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + String.format("%04d", actorId == null ? 0 : actorId % 10000);
        String candidate = baseCode;
        int attempt = 1;
        while (goodsReceiptRepository.existsByReceiptCode(candidate)) candidate = baseCode + "-" + attempt++;
        return candidate;
    }

    private BigDecimal calculateAverageCost(BigDecimal beforeQuantity, BigDecimal beforeAverageCost, BigDecimal importQuantity, BigDecimal importUnitCost) {
        BigDecimal normalizedBeforeQuantity = defaultIfNull(beforeQuantity);
        BigDecimal normalizedBeforeAverageCost = defaultIfNull(beforeAverageCost);
        BigDecimal normalizedImportQuantity = defaultIfNull(importQuantity);
        BigDecimal normalizedImportUnitCost = defaultIfNull(importUnitCost);
        BigDecimal afterQuantity = normalizedBeforeQuantity.add(normalizedImportQuantity);
        if (afterQuantity.compareTo(BigDecimal.ZERO) <= 0) return ZERO;
        if (normalizedBeforeQuantity.compareTo(BigDecimal.ZERO) <= 0) return money(normalizedImportUnitCost);
        BigDecimal beforeValue = normalizedBeforeQuantity.multiply(normalizedBeforeAverageCost);
        BigDecimal importValue = normalizedImportQuantity.multiply(normalizedImportUnitCost);
        return money(beforeValue.add(importValue).divide(afterQuantity, 2, RoundingMode.HALF_UP));
    }

    private BigDecimal sumPeriods(List<InventoryMovementPeriodResponse> periods, java.util.function.Function<InventoryMovementPeriodResponse, BigDecimal> extractor) {
        return periods.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumValues(BigDecimal... values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) total = total.add(defaultIfNull(value));
        return total;
    }

    private Long defaultIfNull(Long value) { return value == null ? 0L : value; }
    private BigDecimal defaultIfNull(BigDecimal value) { return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal quantity(BigDecimal value) { return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal money(BigDecimal value) { return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal rate(BigDecimal value) { return value == null ? ONE_RATE : value.setScale(6, RoundingMode.HALF_UP); }

    private boolean hasRole(CustomUserDetails currentUser, String role) {
        return currentUser != null && role.equalsIgnoreCase(currentUser.getRoleName());
    }

    private void requireAnyRole(CustomUserDetails currentUser, String... roles) {
        for (String role : roles) if (hasRole(currentUser, role)) return;
        throw new ForbiddenException("You do not have permission to perform this action");
    }

    private record ReceiptLine(BigDecimal purchaseQuantity, MeasurementUnit purchaseUnit, MeasurementUnit baseUnit,
                               BigDecimal conversionRate, BigDecimal convertedQuantity, BigDecimal baseUnitCost,
                               BigDecimal lineTotal, boolean fromPurchaseUnit) {
    }
}
