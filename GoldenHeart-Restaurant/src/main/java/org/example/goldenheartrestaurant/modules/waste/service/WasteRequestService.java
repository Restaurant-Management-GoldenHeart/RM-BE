package org.example.goldenheartrestaurant.modules.waste.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.entity.UserProfile;
import org.example.goldenheartrestaurant.modules.identity.repository.UserProfileRepository;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.example.goldenheartrestaurant.modules.inventory.entity.Ingredient;
import org.example.goldenheartrestaurant.modules.inventory.entity.Inventory;
import org.example.goldenheartrestaurant.modules.inventory.entity.InventoryActionLog;
import org.example.goldenheartrestaurant.modules.inventory.entity.InventoryActionType;
import org.example.goldenheartrestaurant.modules.inventory.entity.StockMovement;
import org.example.goldenheartrestaurant.modules.inventory.entity.StockMovementType;
import org.example.goldenheartrestaurant.modules.inventory.repository.IngredientRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.InventoryActionLogRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.InventoryRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.StockMovementRepository;
import org.example.goldenheartrestaurant.modules.menu.service.MenuItemImageStorageService;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.example.goldenheartrestaurant.modules.restaurant.repository.BranchRepository;
import org.example.goldenheartrestaurant.modules.waste.dto.request.CreateWasteRequestItemDto;
import org.example.goldenheartrestaurant.modules.waste.dto.request.CreateWasteRequestRequest;
import org.example.goldenheartrestaurant.modules.waste.dto.request.ReviewWasteRequestRequest;
import org.example.goldenheartrestaurant.modules.waste.dto.response.WasteRequestItemResponse;
import org.example.goldenheartrestaurant.modules.waste.dto.response.WasteRequestResponse;
import org.example.goldenheartrestaurant.modules.waste.dto.response.WasteRequestSummaryResponse;
import org.example.goldenheartrestaurant.modules.waste.entity.WasteReason;
import org.example.goldenheartrestaurant.modules.waste.entity.WasteRequest;
import org.example.goldenheartrestaurant.modules.waste.entity.WasteRequestItem;
import org.example.goldenheartrestaurant.modules.waste.entity.WasteRequestStatus;
import org.example.goldenheartrestaurant.modules.waste.repository.WasteRequestRepository;
import org.springframework.data.domain.Page;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.goldenheartrestaurant.modules.inventory.entity.StockMovement;
import org.example.goldenheartrestaurant.modules.inventory.repository.StockMovementRepository;
import org.example.goldenheartrestaurant.modules.waste.dto.response.WasteStatsResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WasteRequestService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";

    private final WasteRequestRepository wasteRequestRepository;
    private final IngredientRepository ingredientRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryActionLogRepository inventoryActionLogRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final BranchRepository branchRepository;
    private final MenuItemImageStorageService imageStorageService;

    @Transactional
    public WasteRequestResponse create(
            CreateWasteRequestRequest request,
            List<MultipartFile> images,
            CustomUserDetails currentUser
    ) {
        Integer branchId = resolveBranchId(request.branchId(), currentUser);
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chi nhánh"));
        User actor = loadUser(currentUser.getUserId());

        List<WasteRequestItem> items = buildItems(request.items());

        List<String> imageUrls = uploadImages(images, branchId);

        WasteRequest wasteRequest = WasteRequest.builder()
                .branch(branch)
                .requestedBy(actor)
                .status(WasteRequestStatus.PENDING)
                .note(request.note())
                .imageUrls(imageUrls)
                .items(new ArrayList<>())
                .build();

        for (WasteRequestItem item : items) {
            item.setWasteRequest(wasteRequest);
            wasteRequest.getItems().add(item);
        }

        return toResponse(wasteRequestRepository.save(wasteRequest));
    }

    @Transactional(readOnly = true)
    public PageResponse<WasteRequestSummaryResponse> list(
            Integer branchId,
            WasteRequestStatus status,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size,
            CustomUserDetails currentUser
    ) {
        Integer scopedBranchId = resolveScopedBranchId(branchId, currentUser);
        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to   = dateTo   != null ? dateTo.atTime(23, 59, 59) : null;
        Page<WasteRequest> result = wasteRequestRepository.findAllFiltered(
                scopedBranchId, status, from, to,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResponse.<WasteRequestSummaryResponse>builder()
                .content(result.getContent().stream().map(this::toSummaryResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public WasteStatsResponse getStats(Integer branchId, LocalDate dateFrom, LocalDate dateTo, CustomUserDetails currentUser) {
        Integer scopedBranchId = resolveScopedBranchId(branchId, currentUser);
        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to   = dateTo   != null ? dateTo.atTime(23, 59, 59) : null;
        long pending  = wasteRequestRepository.countFiltered(scopedBranchId, WasteRequestStatus.PENDING,  from, to);
        long approved = wasteRequestRepository.countFiltered(scopedBranchId, WasteRequestStatus.APPROVED, from, to);
        long rejected = wasteRequestRepository.countFiltered(scopedBranchId, WasteRequestStatus.REJECTED, from, to);
        BigDecimal wastedValue = stockMovementRepository.sumWasteOutCost(scopedBranchId, from, to);
        return new WasteStatsResponse(pending, approved, rejected,
                wastedValue != null ? wastedValue.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(Integer branchId, LocalDate dateFrom, LocalDate dateTo, CustomUserDetails currentUser) {
        Integer scopedBranchId = resolveScopedBranchId(branchId, currentUser);
        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to   = dateTo   != null ? dateTo.atTime(23, 59, 59) : null;
        List<WasteRequest> approvedList = wasteRequestRepository.findApprovedForExport(scopedBranchId, from, to);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Xuat huy kho");
            DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            DateTimeFormatter dFmt  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // ── Styles ───────────────────────────────────────────────────────────
            Font boldFont = wb.createFont(); boldFont.setBold(true);
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(boldFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN); headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);  headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.cloneStyleFrom(headerStyle);
            moneyStyle.setFillForegroundColor(IndexedColors.WHITE1.getIndex());
            moneyStyle.setAlignment(HorizontalAlignment.RIGHT);
            moneyStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
            CellStyle numStyle = wb.createCellStyle(); numStyle.cloneStyleFrom(moneyStyle);
            numStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            CellStyle totalRowStyle = wb.createCellStyle(); totalRowStyle.setFont(boldFont);
            totalRowStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalRowStyle.setBorderBottom(BorderStyle.THIN); totalRowStyle.setBorderTop(BorderStyle.THIN);
            totalRowStyle.setBorderLeft(BorderStyle.THIN);  totalRowStyle.setBorderRight(BorderStyle.THIN);
            CellStyle totalMoneyStyle = wb.createCellStyle(); totalMoneyStyle.cloneStyleFrom(totalRowStyle);
            totalMoneyStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
            totalMoneyStyle.setAlignment(HorizontalAlignment.RIGHT);

            // ── Title ────────────────────────────────────────────────────────────
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            Font titleFont = wb.createFont(); titleFont.setBold(true); titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = wb.createCellStyle(); titleStyle.setFont(titleFont);
            titleCell.setCellValue("DANH SÁCH XUẤT HỦY KHO");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 7));

            String rangeText = (dateFrom != null ? "Từ " + dateFrom.format(dFmt) : "") +
                               (dateTo   != null ? " đến " + dateTo.format(dFmt) : "");
            if (!rangeText.isBlank()) {
                Row rangeRow = sheet.createRow(1);
                rangeRow.createCell(0).setCellValue(rangeText.trim());
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 7));
            }

            // ── Header ───────────────────────────────────────────────────────────
            String[] headers = {"STT", "Phiếu #", "Tên nguyên liệu", "Đơn vị", "Số lượng hủy",
                                 "Giá vốn / đơn vị", "Thành tiền", "Lý do", "Ngày duyệt"};
            int headerRowIdx = 3;
            Row headerRow = sheet.createRow(headerRowIdx);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            int[] colWidths = {1800, 2000, 7000, 2500, 3500, 4500, 4500, 5000, 5000};
            for (int i = 0; i < colWidths.length; i++) sheet.setColumnWidth(i, colWidths[i]);

            // ── Data rows ────────────────────────────────────────────────────────
            int rowIdx = headerRowIdx + 1;
            int stt = 1;
            BigDecimal grandTotalQty   = BigDecimal.ZERO;
            BigDecimal grandTotalValue = BigDecimal.ZERO;

            for (WasteRequest wr : approvedList) {
                for (WasteRequestItem item : wr.getItems()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(stt++);
                    row.createCell(1).setCellValue(wr.getId());
                    row.createCell(2).setCellValue(item.getIngredientNameSnapshot());
                    row.createCell(3).setCellValue(item.getUnitSymbolSnapshot() != null ? item.getUnitSymbolSnapshot() : "");
                    Cell qtyCell = row.createCell(4);
                    qtyCell.setCellValue(item.getQuantity().doubleValue());
                    qtyCell.setCellStyle(numStyle);

                    // Lấy cost từ StockMovement nếu có, fallback 0
                    BigDecimal unitCost  = BigDecimal.ZERO;
                    BigDecimal totalCost = BigDecimal.ZERO;
                    if (item.getIngredient() != null) {
                        var inv = inventoryRepository.findByBranchIdAndIngredientId(
                                wr.getBranch().getId(), item.getIngredient().getId());
                        if (inv.isPresent() && inv.get().getAverageUnitCost() != null) {
                            unitCost  = inv.get().getAverageUnitCost();
                            totalCost = item.getQuantity().multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
                        }
                    }
                    Cell ucCell = row.createCell(5); ucCell.setCellValue(unitCost.doubleValue()); ucCell.setCellStyle(moneyStyle);
                    Cell tcCell = row.createCell(6); tcCell.setCellValue(totalCost.doubleValue()); tcCell.setCellStyle(moneyStyle);
                    row.createCell(7).setCellValue(REASON_LABELS.getOrDefault(item.getReason() != null ? item.getReason().name() : "", ""));
                    row.createCell(8).setCellValue(wr.getReviewedAt() != null ? wr.getReviewedAt().format(dtFmt) : "");

                    grandTotalQty   = grandTotalQty.add(item.getQuantity());
                    grandTotalValue = grandTotalValue.add(totalCost);
                }
            }

            // ── Total row ────────────────────────────────────────────────────────
            Row totalRow = sheet.createRow(rowIdx);
            for (int i = 0; i < headers.length; i++) {
                Cell c = totalRow.createCell(i);
                c.setCellStyle(i == 6 ? totalMoneyStyle : totalRowStyle);
            }
            totalRow.getCell(2).setCellValue("TỔNG CỘNG");
            totalRow.getCell(4).setCellValue(grandTotalQty.doubleValue());
            totalRow.getCell(6).setCellValue(grandTotalValue.doubleValue());

            sheet.createFreezePane(0, headerRowIdx + 1);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot export waste request Excel", e);
        }
    }

    private static final java.util.Map<String, String> REASON_LABELS = java.util.Map.of(
            "EXPIRED",      "Hết hạn sử dụng",
            "DAMAGED",      "Hư hỏng / vỡ",
            "CONTAMINATED", "Nhiễm khuẩn / ô nhiễm",
            "OTHER",        "Lý do khác"
    );

    @Transactional(readOnly = true)
    public WasteRequestResponse getById(Integer id, CustomUserDetails currentUser) {
        WasteRequest wr = loadDetailOrThrow(id);
        enforceReadScope(wr, currentUser);
        return toResponse(wr);
    }

    @Transactional
    public WasteRequestResponse approve(
            Integer id,
            ReviewWasteRequestRequest request,
            CustomUserDetails currentUser
    ) {
        WasteRequest wr = loadDetailOrThrow(id);
        enforceReviewScope(wr, currentUser);

        if (wr.getStatus() != WasteRequestStatus.PENDING) {
            throw new ConflictException("Chỉ có thể duyệt phiếu đang ở trạng thái chờ duyệt");
        }

        User reviewer = loadUser(currentUser.getUserId());
        Branch branch = wr.getBranch();
        LocalDateTime now = LocalDateTime.now();

        for (WasteRequestItem item : wr.getItems()) {
            Inventory inventory = inventoryRepository
                    .findForUpdateByBranchIdAndIngredientId(branch.getId(), item.getIngredient().getId())
                    .orElseThrow(() -> new NotFoundException(
                            "Không tìm thấy tồn kho cho nguyên liệu: " + item.getIngredientNameSnapshot()
                    ));

            BigDecimal before = inventory.getQuantity() != null ? inventory.getQuantity() : BigDecimal.ZERO;
            BigDecimal quantityChange = item.getQuantity().negate();
            BigDecimal balanceAfter = before.add(quantityChange);

            inventory.setQuantity(balanceAfter);
            inventoryRepository.save(inventory);

            BigDecimal avgCost = inventory.getAverageUnitCost() != null ? inventory.getAverageUnitCost() : BigDecimal.ZERO;
            StockMovement movement = StockMovement.builder()
                    .branch(branch)
                    .ingredient(item.getIngredient())
                    .createdBy(reviewer)
                    .movementType(StockMovementType.WASTE_OUT)
                    .quantityChange(quantityChange)
                    .balanceAfter(balanceAfter)
                    .unitCost(avgCost)
                    .totalCost(item.getQuantity().multiply(avgCost).setScale(2, java.math.RoundingMode.HALF_UP))
                    .occurredAt(now)
                    .note(buildMovementNote(wr.getId(), item.getIngredientNameSnapshot(), item.getReason()))
                    .build();
            stockMovementRepository.save(movement);

            InventoryActionLog actionLog = InventoryActionLog.builder()
                    .inventoryId(inventory.getId())
                    .branchId(branch.getId())
                    .branchName(branch.getName())
                    .ingredientId(item.getIngredient().getId())
                    .ingredientName(item.getIngredientNameSnapshot())
                    .unitSymbol(item.getUnitSymbolSnapshot())
                    .actedBy(reviewer)
                    .actedByUsername(reviewer.getUsername())
                    .actedByFullName(resolveUserName(reviewer))
                    .actionType(InventoryActionType.UPDATED)
                    .beforeQuantity(before)
                    .afterQuantity(balanceAfter)
                    .summary("Xuất hủy kho – Phiếu #" + wr.getId() + " – " + item.getIngredientNameSnapshot()
                            + " [" + translateReason(item.getReason()) + "]")
                    .occurredAt(now)
                    .build();
            inventoryActionLogRepository.save(actionLog);
        }

        wr.setStatus(WasteRequestStatus.APPROVED);
        wr.setReviewedBy(reviewer);
        wr.setReviewedAt(now);
        wr.setReviewNote(request != null ? request.reviewNote() : null);

        return toResponse(wasteRequestRepository.save(wr));
    }

    @Transactional
    public WasteRequestResponse reject(
            Integer id,
            ReviewWasteRequestRequest request,
            CustomUserDetails currentUser
    ) {
        WasteRequest wr = loadDetailOrThrow(id);
        enforceReviewScope(wr, currentUser);

        if (wr.getStatus() != WasteRequestStatus.PENDING) {
            throw new ConflictException("Chỉ có thể từ chối phiếu đang ở trạng thái chờ duyệt");
        }

        User reviewer = loadUser(currentUser.getUserId());
        wr.setStatus(WasteRequestStatus.REJECTED);
        wr.setReviewedBy(reviewer);
        wr.setReviewedAt(LocalDateTime.now());
        wr.setReviewNote(request != null ? request.reviewNote() : null);

        return toResponse(wasteRequestRepository.save(wr));
    }

    @Transactional(readOnly = true)
    public long countPending(CustomUserDetails currentUser) {
        if (ROLE_ADMIN.equals(currentUser.getRoleName())) {
            return wasteRequestRepository.countByStatus(WasteRequestStatus.PENDING);
        }
        Integer branchId = getOwnBranchId(currentUser);
        return wasteRequestRepository.countByBranchIdAndStatus(branchId, WasteRequestStatus.PENDING);
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private List<WasteRequestItem> buildItems(List<CreateWasteRequestItemDto> dtos) {
        List<WasteRequestItem> items = new ArrayList<>();
        for (CreateWasteRequestItemDto dto : dtos) {
            Ingredient ingredient = ingredientRepository.findById(dto.ingredientId())
                    .orElseThrow(() -> new NotFoundException(
                            "Không tìm thấy nguyên liệu với ID: " + dto.ingredientId()
                    ));
            items.add(WasteRequestItem.builder()
                    .ingredient(ingredient)
                    .ingredientNameSnapshot(ingredient.getName())
                    .unitSymbolSnapshot(ingredient.resolveUnitSymbol())
                    .quantity(dto.quantity())
                    .reason(dto.reason())
                    .note(dto.note())
                    .build());
        }
        return items;
    }

    private List<String> uploadImages(List<MultipartFile> images, Integer branchId) {
        if (images == null || images.isEmpty()) return new ArrayList<>();
        List<String> urls = new ArrayList<>();
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) continue;
            MenuItemImageStorageService.StoredMenuItemImage stored =
                    imageStorageService.uploadMenuItemImage(image, branchId, "waste-evidence");
            if (stored != null) {
                urls.add(stored.imageUrl());
            }
        }
        return urls;
    }

    private Integer resolveBranchId(Integer requestBranchId, CustomUserDetails currentUser) {
        if (ROLE_ADMIN.equals(currentUser.getRoleName())) {
            if (requestBranchId == null) {
                throw new ConflictException("branchId là bắt buộc với tài khoản ADMIN");
            }
            return requestBranchId;
        }
        return getOwnBranchId(currentUser);
    }

    private Integer resolveScopedBranchId(Integer requestBranchId, CustomUserDetails currentUser) {
        if (ROLE_ADMIN.equals(currentUser.getRoleName())) {
            return requestBranchId;
        }
        return getOwnBranchId(currentUser);
    }

    private Integer getOwnBranchId(CustomUserDetails currentUser) {
        return userProfileRepository.findActiveDetailByUserId(currentUser.getUserId())
                .map(UserProfile::getBranch)
                .filter(b -> b != null)
                .map(Branch::getId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản chưa được gán chi nhánh"));
    }

    private void enforceReadScope(WasteRequest wr, CustomUserDetails currentUser) {
        if (ROLE_ADMIN.equals(currentUser.getRoleName())) return;
        Integer myBranchId = getOwnBranchId(currentUser);
        if (!wr.getBranch().getId().equals(myBranchId)) {
            throw new ForbiddenException("Không có quyền xem phiếu xuất hủy này");
        }
    }

    private void enforceReviewScope(WasteRequest wr, CustomUserDetails currentUser) {
        if (ROLE_ADMIN.equals(currentUser.getRoleName())) return;
        if (ROLE_MANAGER.equals(currentUser.getRoleName())) {
            Integer myBranchId = getOwnBranchId(currentUser);
            if (!wr.getBranch().getId().equals(myBranchId)) {
                throw new ForbiddenException("Không có quyền duyệt phiếu của chi nhánh khác");
            }
            return;
        }
        throw new ForbiddenException("Không có quyền thực hiện hành động này");
    }

    private WasteRequest loadDetailOrThrow(Integer id) {
        return wasteRequestRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu xuất hủy #" + id));
    }

    private User loadUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
    }

    private String buildMovementNote(Integer wasteRequestId, String ingredientName, WasteReason reason) {
        return "Phiếu xuất hủy #" + wasteRequestId + " - " + ingredientName + " [" + reason.name() + "]";
    }

    private String translateReason(WasteReason reason) {
        return switch (reason) {
            case EXPIRED -> "Hết hạn";
            case DAMAGED -> "Hư hỏng";
            case CONTAMINATED -> "Nhiễm bẩn";
            case OTHER -> "Khác";
        };
    }

    private String resolveUserName(User user) {
        if (user == null) return null;
        UserProfile profile = user.getProfile();
        if (profile != null && StringUtils.hasText(profile.getFullName())) {
            return profile.getFullName();
        }
        return user.getUsername();
    }

    // ─── Mappers ───────────────────────────────────────────────────────────────

    private WasteRequestResponse toResponse(WasteRequest wr) {
        return WasteRequestResponse.builder()
                .id(wr.getId())
                .branchId(wr.getBranch().getId())
                .branchName(wr.getBranch().getName())
                .requestedById(wr.getRequestedBy().getId())
                .requestedByName(resolveUserName(wr.getRequestedBy()))
                .reviewedById(wr.getReviewedBy() != null ? wr.getReviewedBy().getId() : null)
                .reviewedByName(resolveUserName(wr.getReviewedBy()))
                .status(wr.getStatus().name())
                .note(wr.getNote())
                .reviewNote(wr.getReviewNote())
                .reviewedAt(wr.getReviewedAt())
                .createdAt(wr.getCreatedAt())
                .imageUrls(wr.getImageUrls() != null ? wr.getImageUrls() : List.of())
                .items(wr.getItems() != null
                        ? wr.getItems().stream().map(this::toItemResponse).toList()
                        : List.of())
                .build();
    }

    private WasteRequestSummaryResponse toSummaryResponse(WasteRequest wr) {
        return WasteRequestSummaryResponse.builder()
                .id(wr.getId())
                .branchId(wr.getBranch().getId())
                .branchName(wr.getBranch().getName())
                .requestedById(wr.getRequestedBy().getId())
                .requestedByName(resolveUserName(wr.getRequestedBy()))
                .status(wr.getStatus().name())
                .note(wr.getNote())
                .createdAt(wr.getCreatedAt())
                .build();
    }

    private WasteRequestItemResponse toItemResponse(WasteRequestItem item) {
        return WasteRequestItemResponse.builder()
                .id(item.getId())
                .ingredientId(item.getIngredient().getId())
                .ingredientNameSnapshot(item.getIngredientNameSnapshot())
                .unitSymbolSnapshot(item.getUnitSymbolSnapshot())
                .quantity(item.getQuantity())
                .reason(item.getReason().name())
                .note(item.getNote())
                .build();
    }
}
