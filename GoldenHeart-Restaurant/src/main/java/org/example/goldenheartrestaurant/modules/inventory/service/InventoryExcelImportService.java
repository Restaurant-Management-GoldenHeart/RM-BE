package org.example.goldenheartrestaurant.modules.inventory.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventoryImportCommitResponse;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventoryImportPreviewResponse;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventoryImportRowResponse;
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
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.example.goldenheartrestaurant.modules.restaurant.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class InventoryExcelImportService {

    private static final int HEADER_ROW_INDEX = 0;
    private static final int MAX_IMPORT_ROWS = 1000;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_RATE = BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP);
    private static final List<ImportColumn> COLUMNS = List.of(
            new ImportColumn(0, "Tên nguyên liệu", true),
            new ImportColumn(1, "Đơn vị tồn kho", true),
            new ImportColumn(2, "Số lượng mua", true),
            new ImportColumn(3, "Đơn vị mua", true),
            new ImportColumn(4, "Quy đổi mỗi đơn vị", false),
            new ImportColumn(5, "Tổng lượng quy đổi", false),
            new ImportColumn(6, "Đơn giá mua", true),
            new ImportColumn(7, "Tồn tối thiểu", false),
            new ImportColumn(8, "Mức đặt lại", false),
            new ImportColumn(9, "Hạn dùng", false),
            new ImportColumn(10, "Lô hàng", false),
            new ImportColumn(11, "Ghi chú", false)
    );
    private static final int HIDDEN_INGREDIENT_ID_COL = 12;
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    );

    private final InventoryRepository inventoryRepository;
    private final IngredientRepository ingredientRepository;
    private final MeasurementUnitRepository measurementUnitRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryActionLogRepository inventoryActionLogRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public byte[] buildImportTemplate(Integer branchId, CustomUserDetails currentUser) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // Fetch units once — used for "Don vi" sheet and dropdown validation
            List<MeasurementUnit> units = measurementUnitRepository.findAll().stream()
                    .sorted(Comparator.comparing(u -> u.getSymbol().toLowerCase(Locale.ROOT))).toList();

            // Fetch branch inventory for pre-fill (optional)
            List<Inventory> prefillRows = List.of();
            if (branchId != null) {
                if (currentUser != null) ensureCanImportBranch(resolveBranch(branchId), currentUser);
                prefillRows = inventoryRepository.search(null, branchId, false, PageRequest.of(0, 5000))
                        .getContent().stream()
                        .sorted(Comparator.comparing(inv -> inv.getIngredient().getName().toLowerCase(Locale.ROOT)))
                        .toList();
            }

            // ── Styles ──────────────────────────────────────────────────────────
            CellStyle headerStyle         = createHeaderStyle(workbook, IndexedColors.GREY_25_PERCENT);
            CellStyle requiredHeaderStyle = createHeaderStyle(workbook, IndexedColors.LIGHT_ORANGE);
            CreationHelper ch = workbook.getCreationHelper();

            // lockedText: PALE_BLUE tint for pre-filled read-only cells (locked=true is Excel default)
            CellStyle lockedText = workbook.createCellStyle();
            lockedText.setBorderBottom(BorderStyle.THIN); lockedText.setBorderTop(BorderStyle.THIN);
            lockedText.setBorderLeft(BorderStyle.THIN);   lockedText.setBorderRight(BorderStyle.THIN);
            lockedText.setVerticalAlignment(VerticalAlignment.CENTER);
            lockedText.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            lockedText.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // editableXxx: same base style but locked=false so users can type when sheet is protected
            CellStyle editText   = editableStyle(workbook, createTextStyle(workbook));
            CellStyle editNumber = editableStyle(workbook, createTextStyle(workbook));
            editNumber.setDataFormat(ch.createDataFormat().getFormat("#,##0.00"));
            CellStyle editRate   = editableStyle(workbook, createTextStyle(workbook));
            editRate.setDataFormat(ch.createDataFormat().getFormat("#,##0.000000"));
            CellStyle editMoney  = editableStyle(workbook, createTextStyle(workbook));
            editMoney.setDataFormat(ch.createDataFormat().getFormat("#,##0"));
            CellStyle editDate   = editableStyle(workbook, createTextStyle(workbook));
            editDate.setDataFormat(ch.createDataFormat().getFormat("yyyy-mm-dd"));

            // ── Sheet order: guide → units → import (so "Don vi" exists for DataValidation ref)
            writeGuideSheet(workbook.createSheet("Huong dan"), workbook);
            writeUnitSheet(workbook.createSheet("Don vi"), workbook, units);
            Sheet importSheet = workbook.createSheet("Nhap kho");

            // ── Header row ───────────────────────────────────────────────────────
            Row headerRow = importSheet.createRow(HEADER_ROW_INDEX);
            for (ImportColumn column : COLUMNS) {
                Cell cell = headerRow.createCell(column.index());
                cell.setCellValue(column.required() ? column.header() + " *" : column.header());
                cell.setCellStyle(column.required() ? requiredHeaderStyle : headerStyle);
                importSheet.setColumnWidth(column.index(), switch (column.index()) {
                    case 0 -> 6400;
                    case 11 -> 9000;
                    default -> 4300;
                });
            }
            // Hidden "ingredient_id" column — identifies pre-filled ingredient reliably on import
            headerRow.createCell(HIDDEN_INGREDIENT_ID_COL).setCellValue("ingredient_id");
            importSheet.setColumnHidden(HIDDEN_INGREDIENT_ID_COL, true);

            boolean hasPrefill = !prefillRows.isEmpty();

            if (hasPrefill) {
                // ── Pre-filled rows from branch inventory ──────────────────────────
                for (int i = 0; i < prefillRows.size(); i++) {
                    Inventory inv  = prefillRows.get(i);
                    Ingredient ing = inv.getIngredient();
                    Row row = importSheet.createRow(i + 1);

                    // Cols 0,1 locked — prevent accidental name/unit edits
                    createLockedCell(row, 0, ing.getName(), lockedText);
                    createLockedCell(row, 1, ing.resolveUnitSymbol(), lockedText);

                    // Col 2 — quantity (must fill)
                    row.createCell(2).setCellStyle(editNumber);

                    // Col 3 — purchase unit, pre-filled with default
                    Cell puCell = row.createCell(3);
                    if (ing.getDefaultPurchaseUnit() != null) puCell.setCellValue(ing.getDefaultPurchaseUnit().getSymbol());
                    puCell.setCellStyle(editText);

                    // Col 4 — conversion rate, pre-filled with default
                    Cell rateCell = row.createCell(4);
                    if (ing.getDefaultPurchaseToBaseRate() != null) rateCell.setCellValue(ing.getDefaultPurchaseToBaseRate().doubleValue());
                    rateCell.setCellStyle(editRate);

                    // Col 5 — converted qty (optional override)
                    row.createCell(5).setCellStyle(editNumber);

                    // Col 6 — purchase unit cost (must fill)
                    row.createCell(6).setCellStyle(editMoney);

                    // Cols 7,8 — min stock, reorder pre-filled from current settings
                    Cell minCell = row.createCell(7);
                    if (inv.getMinStockLevel() != null) minCell.setCellValue(inv.getMinStockLevel().doubleValue());
                    minCell.setCellStyle(editNumber);
                    Cell reorderCell = row.createCell(8);
                    if (inv.getReorderLevel() != null) reorderCell.setCellValue(inv.getReorderLevel().doubleValue());
                    reorderCell.setCellStyle(editNumber);

                    // Cols 9–11 — expiry, batch, note (blank, user fills)
                    row.createCell(9).setCellStyle(editDate);
                    row.createCell(10).setCellStyle(editText);
                    row.createCell(11).setCellStyle(editText);

                    // Col 12 hidden, locked — ingredient ID for reliable lookup on commit
                    Cell idCell = row.createCell(HIDDEN_INGREDIENT_ID_COL);
                    idCell.setCellValue(ing.getId());
                    idCell.setCellStyle(lockedText);
                }

                // ── 20 blank rows at bottom for new ingredients ────────────────────
                int blankStart = prefillRows.size() + 1;
                for (int i = blankStart; i < blankStart + 20; i++) {
                    Row row = importSheet.createRow(i);
                    row.createCell(0).setCellStyle(editText);
                    row.createCell(1).setCellStyle(editText);
                    row.createCell(2).setCellStyle(editNumber);
                    row.createCell(3).setCellStyle(editText);
                    row.createCell(4).setCellStyle(editRate);
                    row.createCell(5).setCellStyle(editNumber);
                    row.createCell(6).setCellStyle(editMoney);
                    row.createCell(7).setCellStyle(editNumber);
                    row.createCell(8).setCellStyle(editNumber);
                    row.createCell(9).setCellStyle(editDate);
                    row.createCell(10).setCellStyle(editText);
                    row.createCell(11).setCellStyle(editText);
                }

                // Protect sheet so locked cells (name, unit, id) cannot be edited
                importSheet.protectSheet("");
                addUnitDropdown(importSheet, units.size(), 1, prefillRows.size() + 20);

            } else {
                // ── Blank template — same behaviour as before ──────────────────────
                for (int rowIndex = 1; rowIndex <= 200; rowIndex++) {
                    Row row = importSheet.createRow(rowIndex);
                    for (ImportColumn column : COLUMNS) {
                        Cell cell = row.createCell(column.index());
                        if (List.of(2, 5, 7, 8).contains(column.index())) cell.setCellStyle(editNumber);
                        else if (column.index() == 4) cell.setCellStyle(editRate);
                        else if (column.index() == 6) cell.setCellStyle(editMoney);
                        else if (column.index() == 9) cell.setCellStyle(editDate);
                        else cell.setCellStyle(editText);
                    }
                }
                addUnitDropdown(importSheet, units.size(), 1, 200);
            }

            importSheet.createFreezePane(0, 1);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot build inventory import template", exception);
        }
    }

    @Transactional(readOnly = true)
    public InventoryImportPreviewResponse previewImport(MultipartFile file, Integer branchId, LocalDate receiptDate,
                                                        String invoiceNumber, String note, CustomUserDetails currentUser) {
        return parseImportFile(file, branchId, receiptDate, invoiceNumber, note, currentUser).preview();
    }

    @Transactional
    public InventoryImportCommitResponse commitImport(MultipartFile file, Integer branchId, LocalDate receiptDate,
                                                       String invoiceNumber, String note, CustomUserDetails currentUser) {
        ParsedImportFile parsedFile = parseImportFile(file, branchId, receiptDate, invoiceNumber, note, currentUser);
        if (!parsedFile.preview().importable()) {
            return new InventoryImportCommitResponse(false, null, null, parsedFile.preview());
        }
        User actor = resolveActor(currentUser.getUserId());
        Branch branch = parsedFile.branch();
        LocalDateTime now = LocalDateTime.now();
        String receiptCode = generateReceiptCode(now, actor.getId());
        GoodsReceipt receipt = goodsReceiptRepository.save(GoodsReceipt.builder()
                .receiptCode(receiptCode).branch(branch).receivedBy(actor).receiptDate(parsedFile.receiptDate())
                .invoiceNumber(parsedFile.invoiceNumber()).subtotalAmount(parsedFile.preview().totalAmount()).taxAmount(ZERO)
                .totalAmount(parsedFile.preview().totalAmount()).status(GoodsReceiptStatus.RECEIVED).note(parsedFile.note())
                .createdAt(now).items(new ArrayList<>()).build());
        for (ParsedImportRow row : parsedFile.rows()) {
            if (row.valid()) applyImportRow(branch, receipt, actor, row, now);
        }
        GoodsReceipt savedReceipt = goodsReceiptRepository.save(receipt);
        return new InventoryImportCommitResponse(true, savedReceipt.getId(), savedReceipt.getReceiptCode(), parsedFile.preview());
    }

    private ParsedImportFile parseImportFile(MultipartFile file, Integer branchId, LocalDate receiptDate,
                                             String invoiceNumber, String note, CustomUserDetails currentUser) {
        Branch branch = resolveBranch(branchId);
        ensureCanImportBranch(branch, currentUser);
        LocalDate effectiveReceiptDate = receiptDate != null ? receiptDate : LocalDate.now();
        String normalizedInvoiceNumber = cleanText(invoiceNumber);
        String normalizedNote = cleanText(note);
        List<String> globalErrors = new ArrayList<>();
        List<ParsedImportRow> parsedRows = new ArrayList<>();
        if (normalizedInvoiceNumber != null && normalizedInvoiceNumber.length() > 50) globalErrors.add("Số hóa đơn không được vượt quá 50 ký tự.");
        if (normalizedNote != null && normalizedNote.length() > 500) globalErrors.add("Ghi chú phiếu nhập không được vượt quá 500 ký tự.");
        if (effectiveReceiptDate.isAfter(LocalDate.now())) globalErrors.add("Ngày nhập không được sau ngày hiện tại.");
        if (file == null || file.isEmpty()) {
            globalErrors.add("Chưa chọn file Excel để import.");
            return buildParsedFile(branch, effectiveReceiptDate, normalizedInvoiceNumber, normalizedNote, globalErrors, parsedRows);
        }
        if (file.getSize() > 5L * 1024L * 1024L) {
            globalErrors.add("File import tối đa 5MB.");
            return buildParsedFile(branch, effectiveReceiptDate, normalizedInvoiceNumber, normalizedNote, globalErrors, parsedRows);
        }
        if (!hasExcelExtension(file.getOriginalFilename())) {
            globalErrors.add("File import phải có định dạng .xlsx hoặc .xls.");
            return buildParsedFile(branch, effectiveReceiptDate, normalizedInvoiceNumber, normalizedNote, globalErrors, parsedRows);
        }
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                globalErrors.add("File Excel không có sheet dữ liệu.");
                return buildParsedFile(branch, effectiveReceiptDate, normalizedInvoiceNumber, normalizedNote, globalErrors, parsedRows);
            }
            Sheet sheet = workbook.getSheet("Nhap kho");
            if (sheet == null) sheet = workbook.getSheetAt(0); // fallback cho file cũ
            DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("vi-VN"));
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            validateHeader(sheet.getRow(HEADER_ROW_INDEX), formatter, evaluator, globalErrors);
            if (!globalErrors.isEmpty()) return buildParsedFile(branch, effectiveReceiptDate, normalizedInvoiceNumber, normalizedNote, globalErrors, parsedRows);
            Map<String, MeasurementUnit> unitLookup = buildUnitLookup(measurementUnitRepository.findAll());
            Map<String, PreviewStockState> previewStates = new HashMap<>();
            int nonBlankRows = 0;
            for (int rowIndex = HEADER_ROW_INDEX + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isRowBlank(row, formatter, evaluator)) continue;
                nonBlankRows++;
                if (nonBlankRows > MAX_IMPORT_ROWS) {
                    globalErrors.add("File import chỉ hỗ trợ tối đa " + MAX_IMPORT_ROWS + " dòng dữ liệu trong một lần.");
                    break;
                }
                parsedRows.add(parseDataRow(row, formatter, evaluator, branch, effectiveReceiptDate, unitLookup, previewStates));
            }
            if (nonBlankRows == 0) globalErrors.add("File chưa có dòng nguyên liệu nào để import.");
        } catch (Exception exception) {
            globalErrors.add("Không đọc được file Excel. Vui lòng tải file mẫu và nhập lại đúng định dạng.");
        }
        return buildParsedFile(branch, effectiveReceiptDate, normalizedInvoiceNumber, normalizedNote, globalErrors, parsedRows);
    }

    private ParsedImportFile buildParsedFile(Branch branch, LocalDate receiptDate, String invoiceNumber, String note,
                                             List<String> globalErrors, List<ParsedImportRow> parsedRows) {
        List<InventoryImportRowResponse> rowResponses = parsedRows.stream().map(this::toRowResponse).toList();
        int totalRows = parsedRows.size();
        int invalidRows = (int) parsedRows.stream().filter(row -> !row.valid()).count();
        int validRows = totalRows - invalidRows;
        BigDecimal totalQuantity = parsedRows.stream().filter(ParsedImportRow::valid).map(ParsedImportRow::convertedQuantity)
                .reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = parsedRows.stream().filter(ParsedImportRow::valid).map(ParsedImportRow::lineTotal)
                .reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        boolean importable = globalErrors.isEmpty() && totalRows > 0 && invalidRows == 0;
        InventoryImportPreviewResponse preview = new InventoryImportPreviewResponse(branch.getId(), branch.getName(), receiptDate,
                invoiceNumber, note, totalRows, validRows, invalidRows, totalQuantity, totalAmount, importable,
                List.copyOf(globalErrors), rowResponses);
        return new ParsedImportFile(branch, receiptDate, invoiceNumber, note, preview, parsedRows);
    }

    private ParsedImportRow parseDataRow(Row row, DataFormatter formatter, FormulaEvaluator evaluator, Branch branch,
                                         LocalDate receiptDate, Map<String, MeasurementUnit> unitLookup,
                                         Map<String, PreviewStockState> previewStates) {
        int rowNumber = row.getRowNum() + 1;
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Integer prefilledIngredientId = readIngredientIdCell(row);
        String ingredientName = cleanText(cellText(row, 0, formatter, evaluator));
        String baseUnitText = cleanText(cellText(row, 1, formatter, evaluator));
        DecimalCellValue purchaseQuantityValue = parseDecimalCell(row, 2, formatter, evaluator);
        String purchaseUnitText = cleanText(cellText(row, 3, formatter, evaluator));
        DecimalCellValue conversionRateValue = parseDecimalCell(row, 4, formatter, evaluator);
        DecimalCellValue convertedQuantityValue = parseDecimalCell(row, 5, formatter, evaluator);
        DecimalCellValue purchaseUnitCostValue = parseDecimalCell(row, 6, formatter, evaluator);
        DecimalCellValue minStockValue = parseDecimalCell(row, 7, formatter, evaluator);
        DecimalCellValue reorderValue = parseDecimalCell(row, 8, formatter, evaluator);
        DateCellValue expiryDateValue = parseDateCell(row, 9, formatter, evaluator);
        String batchNumber = cleanText(cellText(row, 10, formatter, evaluator));
        String rowNote = cleanText(cellText(row, 11, formatter, evaluator));

        if (!StringUtils.hasText(ingredientName)) errors.add(columnError("Tên nguyên liệu", "bắt buộc nhập."));
        else if (ingredientName.length() > 100) errors.add(columnError("Tên nguyên liệu", "không được vượt quá 100 ký tự."));
        MeasurementUnit baseUnit = resolveUnitFromText(baseUnitText, "Đơn vị tồn kho", unitLookup, errors);
        MeasurementUnit purchaseUnit = resolveUnitFromText(purchaseUnitText, "Đơn vị mua", unitLookup, errors);

        BigDecimal purchaseQuantity = purchaseQuantityValue.value();
        if (!purchaseQuantityValue.present()) errors.add(columnError("Số lượng mua", "bắt buộc nhập."));
        else if (purchaseQuantityValue.error() != null) errors.add(columnError("Số lượng mua", purchaseQuantityValue.error()));
        else if (purchaseQuantity.compareTo(BigDecimal.ZERO) <= 0) errors.add(columnError("Số lượng mua", "phải lớn hơn 0."));

        BigDecimal purchaseUnitCost = purchaseUnitCostValue.value();
        if (!purchaseUnitCostValue.present()) errors.add(columnError("Đơn giá mua", "bắt buộc nhập."));
        else if (purchaseUnitCostValue.error() != null) errors.add(columnError("Đơn giá mua", purchaseUnitCostValue.error()));
        else if (purchaseUnitCost.compareTo(BigDecimal.ZERO) < 0) errors.add(columnError("Đơn giá mua", "không được âm."));

        BigDecimal inputConversionRate = conversionRateValue.value();
        if (conversionRateValue.error() != null) errors.add(columnError("Quy đổi mỗi đơn vị", conversionRateValue.error()));
        else if (inputConversionRate != null && inputConversionRate.compareTo(BigDecimal.ZERO) <= 0) errors.add(columnError("Quy đổi mỗi đơn vị", "phải lớn hơn 0."));
        BigDecimal inputConvertedQuantity = convertedQuantityValue.value();
        if (convertedQuantityValue.error() != null) errors.add(columnError("Tổng lượng quy đổi", convertedQuantityValue.error()));
        else if (inputConvertedQuantity != null && inputConvertedQuantity.compareTo(BigDecimal.ZERO) <= 0) errors.add(columnError("Tổng lượng quy đổi", "phải lớn hơn 0."));

        BigDecimal minStockLevel = minStockValue.value();
        if (minStockValue.error() != null) errors.add(columnError("Tồn tối thiểu", minStockValue.error()));
        else if (minStockLevel != null && minStockLevel.compareTo(BigDecimal.ZERO) < 0) errors.add(columnError("Tồn tối thiểu", "không được âm."));
        BigDecimal reorderLevel = reorderValue.value();
        if (reorderValue.error() != null) errors.add(columnError("Mức đặt lại", reorderValue.error()));
        else if (reorderLevel != null && reorderLevel.compareTo(BigDecimal.ZERO) < 0) errors.add(columnError("Mức đặt lại", "không được âm."));
        LocalDate expiryDate = expiryDateValue.value();
        if (expiryDateValue.error() != null) errors.add(columnError("Hạn dùng", expiryDateValue.error()));
        else if (expiryDate != null && expiryDate.isBefore(receiptDate)) errors.add(columnError("Hạn dùng", "không được trước ngày nhập."));
        if (batchNumber != null && batchNumber.length() > 50) errors.add(columnError("Lô hàng", "không được vượt quá 50 ký tự."));
        if (rowNote != null && rowNote.length() > 255) errors.add(columnError("Ghi chú", "không được vượt quá 255 ký tự."));

        String action = "Không xác định";
        Integer ingredientId = null;
        Integer inventoryId = null;
        BigDecimal currentQuantity = ZERO;
        BigDecimal quantityAfterImport = null;
        BigDecimal averageUnitCostAfterImport = null;
        BigDecimal conversionRate = inputConversionRate;
        BigDecimal convertedQuantity = inputConvertedQuantity;
        BigDecimal lineTotal = purchaseQuantity != null && purchaseUnitCost != null ? money(purchaseQuantity.multiply(purchaseUnitCost)) : null;
        BigDecimal baseUnitCost = null;

        if (errors.isEmpty()) {
            String ingredientKey = normalizeLookupText(ingredientName);
            PreviewStockState state = previewStates.get(ingredientKey);
            if (state == null) {
                state = loadPreviewStockState(branch, ingredientName, baseUnit, prefilledIngredientId, rowNumber, errors);
                if (state != null) previewStates.put(ingredientKey, state);
            } else if (!state.unitId().equals(baseUnit.getId())) {
                errors.add(columnError("Đơn vị tồn kho", "khác với đơn vị đã dùng cho nguyên liệu này ở dòng " + state.firstRowNumber() + "."));
            }
            if (errors.isEmpty() && state != null) {
                ConversionResult conversion = resolveRowConversion(purchaseQuantity, purchaseUnit, baseUnit,
                        conversionRate, convertedQuantity, state, errors);
                conversionRate = conversion.conversionRate();
                convertedQuantity = conversion.convertedQuantity();
                if (errors.isEmpty()) {
                    BigDecimal effectiveMinStock = minStockLevel != null ? quantity(minStockLevel) : state.minStockLevel();
                    BigDecimal effectiveReorder = reorderLevel != null ? quantity(reorderLevel) : state.reorderLevel();
                    if (effectiveReorder.compareTo(effectiveMinStock) < 0) {
                        errors.add("Mức đặt lại phải lớn hơn hoặc bằng tồn tối thiểu sau import.");
                    } else {
                        lineTotal = money(purchaseQuantity.multiply(purchaseUnitCost));
                        baseUnitCost = money(lineTotal.divide(convertedQuantity, 2, RoundingMode.HALF_UP));
                        currentQuantity = state.quantity();
                        quantityAfterImport = quantity(state.quantity().add(convertedQuantity));
                        averageUnitCostAfterImport = calculateAverageCost(state.quantity(), state.averageUnitCost(), convertedQuantity, baseUnitCost);
                        action = state.nextActionLabel();
                        ingredientId = state.ingredientId();
                        inventoryId = state.inventoryId();
                        state.learnDefault(purchaseUnit.getId(), conversionRate);
                        state.applyRow(quantityAfterImport, averageUnitCostAfterImport, effectiveMinStock, effectiveReorder);
                    }
                }
            }
        }

        boolean valid = errors.isEmpty();
        return new ParsedImportRow(rowNumber, valid, ingredientName, baseUnitText, baseUnit, purchaseUnitText, purchaseUnit,
                quantity(purchaseQuantity), rateOrNull(conversionRate), quantity(convertedQuantity), money(purchaseUnitCost),
                money(baseUnitCost), lineTotal, quantity(minStockLevel), quantity(reorderLevel), expiryDate, batchNumber, rowNote,
                action, ingredientId, inventoryId, currentQuantity, quantityAfterImport, averageUnitCostAfterImport,
                List.copyOf(errors), List.copyOf(warnings));
    }

    private MeasurementUnit resolveUnitFromText(String unitText, String columnName, Map<String, MeasurementUnit> unitLookup, List<String> errors) {
        if (!StringUtils.hasText(unitText)) {
            errors.add(columnError(columnName, "bắt buộc nhập."));
            return null;
        }
        MeasurementUnit unit = unitLookup.get(normalizeLookupText(unitText));
        if (unit == null) errors.add(columnError(columnName, "không tồn tại trong hệ thống. Kiểm tra sheet 'Don vi' trong file mẫu."));
        return unit;
    }

    private ConversionResult resolveRowConversion(BigDecimal purchaseQuantity, MeasurementUnit purchaseUnit, MeasurementUnit baseUnit,
                                                  BigDecimal inputRate, BigDecimal inputConvertedQuantity, PreviewStockState state,
                                                  List<String> errors) {
        BigDecimal conversionRate = inputRate != null ? rate(inputRate) : null;
        BigDecimal convertedQuantity = inputConvertedQuantity != null ? quantity(inputConvertedQuantity) : null;
        if (conversionRate == null && convertedQuantity == null) {
            conversionRate = state.defaultRateFor(purchaseUnit.getId());
            if (conversionRate == null && Objects.equals(purchaseUnit.getId(), baseUnit.getId())) conversionRate = ONE_RATE;
            if (conversionRate == null) {
                errors.add(columnError("Quy đổi mỗi đơn vị", "bắt buộc nhập khi đơn vị mua khác đơn vị tồn kho và nguyên liệu chưa có quy đổi mặc định."));
                return new ConversionResult(null, null);
            }
            convertedQuantity = quantity(purchaseQuantity.multiply(conversionRate));
        } else if (conversionRate != null && convertedQuantity == null) {
            convertedQuantity = quantity(purchaseQuantity.multiply(conversionRate));
        } else if (conversionRate == null) {
            conversionRate = rate(convertedQuantity.divide(purchaseQuantity, 6, RoundingMode.HALF_UP));
        } else {
            BigDecimal expected = quantity(purchaseQuantity.multiply(conversionRate));
            if (expected.subtract(convertedQuantity).abs().compareTo(new BigDecimal("0.01")) > 0) {
                errors.add("Quy đổi mỗi đơn vị và Tổng lượng quy đổi không khớp nhau. Hãy giữ một trong hai cột, hoặc sửa lại số liệu.");
            }
        }
        return new ConversionResult(conversionRate, convertedQuantity);
    }

    private PreviewStockState loadPreviewStockState(Branch branch, String ingredientName, MeasurementUnit baseUnit,
                                                    Integer prefilledIngredientId, int firstRowNumber, List<String> errors) {
        // Try pre-filled ID first (from hidden col 12 in template) — avoids name-based lookup issues
        Optional<Ingredient> existingIngredient = Optional.empty();
        if (prefilledIngredientId != null) existingIngredient = ingredientRepository.findById(prefilledIngredientId);
        if (existingIngredient.isEmpty()) existingIngredient = ingredientRepository.findByBranchIdAndNameIgnoreCase(branch.getId(), ingredientName);
        if (existingIngredient.isEmpty()) return PreviewStockState.newIngredient(baseUnit, firstRowNumber);
        Ingredient ingredient = existingIngredient.get();
        Integer existingUnitId = ingredient.getMeasurementUnit() != null ? ingredient.getMeasurementUnit().getId() : null;
        if (existingUnitId != null && !existingUnitId.equals(baseUnit.getId())) {
            errors.add(columnError("Đơn vị tồn kho", "nguyên liệu đã tồn tại với đơn vị " + ingredient.resolveUnitSymbol() + "."));
            return null;
        }
        Optional<Inventory> existingInventory = inventoryRepository.findByBranchIdAndIngredientId(branch.getId(), ingredient.getId());
        Integer defaultPurchaseUnitId = ingredient.getDefaultPurchaseUnit() != null ? ingredient.getDefaultPurchaseUnit().getId() : null;
        BigDecimal defaultRate = ingredient.getDefaultPurchaseToBaseRate() != null ? rate(ingredient.getDefaultPurchaseToBaseRate()) : null;
        if (existingInventory.isEmpty()) {
            return PreviewStockState.existingIngredientWithoutInventory(ingredient, baseUnit, firstRowNumber, defaultPurchaseUnitId, defaultRate);
        }
        return PreviewStockState.existingInventory(ingredient, existingInventory.get(), baseUnit, firstRowNumber, defaultPurchaseUnitId, defaultRate);
    }

    private void applyImportRow(Branch branch, GoodsReceipt receipt, User actor, ParsedImportRow row, LocalDateTime occurredAt) {
        // Use pre-resolved ID (from hidden col 12 in template) for fast, reliable lookup
        Ingredient ingredient = row.ingredientId() != null
                ? ingredientRepository.findById(row.ingredientId())
                        .orElseGet(() -> resolveOrCreateIngredient(branch, row.ingredientName(), row.baseUnit()))
                : resolveOrCreateIngredient(branch, row.ingredientName(), row.baseUnit());
        if (ingredient.getDefaultPurchaseUnit() == null && row.purchaseUnit() != null && row.conversionRate() != null) {
            ingredient.setDefaultPurchaseUnit(row.purchaseUnit());
            ingredient.setDefaultPurchaseToBaseRate(row.conversionRate());
            ingredientRepository.save(ingredient);
        }
        Inventory inventory = inventoryRepository.findForUpdateByBranchIdAndIngredientId(branch.getId(), ingredient.getId()).orElse(null);
        boolean createdInventory = inventory == null;
        BigDecimal beforeQuantity = inventory == null ? ZERO : defaultIfNull(inventory.getQuantity());
        BigDecimal beforeMinStock = inventory == null ? ZERO : defaultIfNull(inventory.getMinStockLevel());
        BigDecimal beforeReorder = inventory == null ? ZERO : defaultIfNull(inventory.getReorderLevel());
        BigDecimal beforeAverageCost = inventory == null ? ZERO : defaultIfNull(inventory.getAverageUnitCost());
        String beforeIngredientName = createdInventory ? null : ingredient.getName();
        String beforeUnitSymbol = createdInventory ? null : ingredient.resolveUnitSymbol();
        BigDecimal afterQuantity = quantity(beforeQuantity.add(row.convertedQuantity()));
        BigDecimal afterAverageCost = calculateAverageCost(beforeQuantity, beforeAverageCost, row.convertedQuantity(), row.baseUnitCost());
        BigDecimal afterMinStock = row.minStockLevel() != null ? row.minStockLevel() : beforeMinStock;
        BigDecimal afterReorder = row.reorderLevel() != null ? row.reorderLevel() : beforeReorder;
        if (inventory == null) {
            inventory = Inventory.builder().branch(branch).ingredient(ingredient).quantity(ZERO).minStockLevel(ZERO)
                    .reorderLevel(ZERO).averageUnitCost(ZERO).build();
        }
        inventory.setQuantity(afterQuantity);
        inventory.setAverageUnitCost(afterAverageCost);
        inventory.setMinStockLevel(afterMinStock);
        inventory.setReorderLevel(afterReorder);
        inventory.setLastReceiptAt(occurredAt);
        Inventory savedInventory = inventoryRepository.save(inventory);

        GoodsReceiptItem item = GoodsReceiptItem.builder().goodsReceipt(receipt).ingredient(ingredient)
                .quantity(row.convertedQuantity()).unitCost(row.baseUnitCost()).lineTotal(row.lineTotal())
                .purchaseQuantity(row.purchaseQuantity()).purchaseUnit(row.purchaseUnit()).conversionRate(row.conversionRate())
                .convertedQuantity(row.convertedQuantity()).baseUnit(row.baseUnit()).batchNumber(row.batchNumber())
                .expiryDate(row.expiryDate()).note(row.note()).build();
        receipt.getItems().add(item);

        stockMovementRepository.save(StockMovement.builder().branch(branch).ingredient(ingredient).goodsReceipt(receipt)
                .createdBy(actor).movementType(StockMovementType.RECEIPT_IN).quantityChange(row.convertedQuantity())
                .balanceAfter(afterQuantity).unitCost(row.baseUnitCost()).totalCost(row.lineTotal()).occurredAt(occurredAt)
                .note(buildImportNote(receipt.getReceiptCode(), row)).build());

        inventoryActionLogRepository.save(InventoryActionLog.builder()
                .inventoryId(savedInventory.getId()).branchId(branch.getId()).branchName(branch.getName())
                .ingredientId(ingredient.getId()).ingredientName(ingredient.getName()).unitSymbol(ingredient.resolveUnitSymbol())
                .actedBy(actor).actedByUsername(actor.getUsername())
                .actedByFullName(actor.getProfile() != null ? actor.getProfile().getFullName() : null)
                .actionType(createdInventory ? InventoryActionType.CREATED : InventoryActionType.UPDATED)
                .beforeQuantity(beforeQuantity).afterQuantity(afterQuantity)
                .beforeMinStockLevel(beforeMinStock).afterMinStockLevel(afterMinStock)
                .beforeReorderLevel(beforeReorder).afterReorderLevel(afterReorder)
                .beforeAverageUnitCost(beforeAverageCost).afterAverageUnitCost(afterAverageCost)
                .beforeIngredientName(beforeIngredientName).afterIngredientName(ingredient.getName())
                .beforeUnitSymbol(beforeUnitSymbol).afterUnitSymbol(ingredient.resolveUnitSymbol())
                .summary("Import Excel phiếu nhập " + receipt.getReceiptCode() + ": +"
                        + row.convertedQuantity().stripTrailingZeros().toPlainString() + " " + ingredient.resolveUnitSymbol()
                        + " từ " + row.purchaseQuantity().stripTrailingZeros().toPlainString() + " " + row.purchaseUnit().getSymbol())
                .occurredAt(occurredAt).build());
    }

    private Ingredient resolveOrCreateIngredient(Branch branch, String ingredientName, MeasurementUnit baseUnit) {
        String normalizedName = ingredientName.trim();
        return ingredientRepository.findByBranchIdAndNameIgnoreCase(branch.getId(), normalizedName)
                .map(existingIngredient -> {
                    Integer existingUnitId = existingIngredient.getMeasurementUnit() != null ? existingIngredient.getMeasurementUnit().getId() : null;
                    if (existingUnitId != null && !existingUnitId.equals(baseUnit.getId())) throw new ConflictException("Ingredient already exists with another unit");
                    if (existingIngredient.getMeasurementUnit() == null) {
                        existingIngredient.setMeasurementUnit(baseUnit);
                        existingIngredient.setLegacyUnit(baseUnit.getSymbol());
                        return ingredientRepository.save(existingIngredient);
                    }
                    return existingIngredient;
                })
                .orElseGet(() -> ingredientRepository.save(Ingredient.builder().name(normalizedName).branch(branch)
                        .measurementUnit(baseUnit).legacyUnit(baseUnit.getSymbol()).build()));
    }

    private InventoryImportRowResponse toRowResponse(ParsedImportRow row) {
        MeasurementUnit baseUnit = row.baseUnit();
        MeasurementUnit purchaseUnit = row.purchaseUnit();
        return new InventoryImportRowResponse(row.rowNumber(), row.valid(), row.ingredientName(), row.baseUnitText(),
                baseUnit != null ? baseUnit.getId() : null, baseUnit != null ? baseUnit.getName() : null,
                baseUnit != null ? baseUnit.getSymbol() : null, row.convertedQuantity(), row.baseUnitCost(), row.lineTotal(),
                row.purchaseQuantity(), row.purchaseUnitText(), purchaseUnit != null ? purchaseUnit.getId() : null,
                purchaseUnit != null ? purchaseUnit.getName() : null, purchaseUnit != null ? purchaseUnit.getSymbol() : null,
                row.conversionRate(), row.convertedQuantity(), row.purchaseUnitCost(), row.minStockLevel(), row.reorderLevel(),
                row.expiryDate(), row.batchNumber(), row.note(), row.action(), row.ingredientId(), row.inventoryId(),
                row.currentQuantity(), row.quantityAfterImport(), row.averageUnitCostAfterImport(), row.errors(), row.warnings());
    }

    private void validateHeader(Row headerRow, DataFormatter formatter, FormulaEvaluator evaluator, List<String> globalErrors) {
        if (headerRow == null) {
            globalErrors.add("Dòng đầu tiên phải là header của file mẫu import kho.");
            return;
        }
        for (ImportColumn column : COLUMNS) {
            String actual = cellText(headerRow, column.index(), formatter, evaluator);
            if (!normalizeHeader(actual).equals(normalizeHeader(column.header()))) {
                globalErrors.add("Header cột " + excelColumnName(column.index()) + " phải là '" + column.header() + "'.");
            }
        }
    }

    private Map<String, MeasurementUnit> buildUnitLookup(List<MeasurementUnit> units) {
        Map<String, MeasurementUnit> lookup = new HashMap<>();
        for (MeasurementUnit unit : units) {
            putUnitLookup(lookup, String.valueOf(unit.getId()), unit);
            putUnitLookup(lookup, unit.getCode(), unit);
            putUnitLookup(lookup, unit.getName(), unit);
            putUnitLookup(lookup, unit.getSymbol(), unit);
        }
        return lookup;
    }

    private void putUnitLookup(Map<String, MeasurementUnit> lookup, String value, MeasurementUnit unit) {
        if (StringUtils.hasText(value)) lookup.putIfAbsent(normalizeLookupText(value), unit);
    }

    private DecimalCellValue parseDecimalCell(Row row, int columnIndex, DataFormatter formatter, FormulaEvaluator evaluator) {
        String raw = cellText(row, columnIndex, formatter, evaluator);
        if (!StringUtils.hasText(raw)) return new DecimalCellValue(null, false, null);
        String cleaned = raw.trim().toLowerCase(Locale.ROOT).replace("vnđ", "").replace("vnd", "")
                .replace("đ", "").replace("₫", "").replaceAll("\\s+", "").replaceAll("[^0-9,.-]", "");
        if (!StringUtils.hasText(cleaned) || cleaned.equals("-") || cleaned.equals(".") || cleaned.equals(",")) {
            return new DecimalCellValue(null, true, "phải là số hợp lệ.");
        }
        try {
            return new DecimalCellValue(new BigDecimal(normalizeNumberString(cleaned)), true, null);
        } catch (NumberFormatException exception) {
            return new DecimalCellValue(null, true, "phải là số hợp lệ.");
        }
    }

    private DateCellValue parseDateCell(Row row, int columnIndex, DataFormatter formatter, FormulaEvaluator evaluator) {
        Cell cell = row.getCell(columnIndex);
        String raw = cellText(row, columnIndex, formatter, evaluator);
        if (!StringUtils.hasText(raw)) return new DateCellValue(null, false, null);
        if (cell != null && DateUtil.isCellDateFormatted(cell)) {
            try {
                return new DateCellValue(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), true, null);
            } catch (Exception ignored) {
                return new DateCellValue(null, true, "phải là ngày hợp lệ.");
            }
        }
        for (DateTimeFormatter dateFormatter : DATE_FORMATTERS) {
            try {
                return new DateCellValue(LocalDate.parse(raw.trim(), dateFormatter), true, null);
            } catch (DateTimeParseException ignored) {
            }
        }
        return new DateCellValue(null, true, "phải theo định dạng yyyy-mm-dd hoặc dd/mm/yyyy.");
    }

    private String normalizeNumberString(String value) {
        boolean hasComma = value.contains(",");
        boolean hasDot = value.contains(".");
        if (hasComma && hasDot) return value.lastIndexOf(',') > value.lastIndexOf('.') ? value.replace(".", "").replace(',', '.') : value.replace(",", "");
        if (hasComma) {
            int decimals = value.length() - value.lastIndexOf(',') - 1;
            return decimals > 0 && decimals <= 6 ? value.replace(',', '.') : value.replace(",", "");
        }
        if (hasDot) {
            int dotCount = value.length() - value.replace(".", "").length();
            int decimals = value.length() - value.lastIndexOf('.') - 1;
            int integerDigits = value.lastIndexOf('.');
            if (dotCount > 1 || (decimals == 3 && integerDigits > 1)) return value.replace(".", "");
        }
        return value;
    }

    private String cellText(Row row, int columnIndex, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) return "";
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return "";
        try {
            return formatter.formatCellValue(cell, evaluator).trim();
        } catch (RuntimeException exception) {
            return formatter.formatCellValue(cell).trim();
        }
    }

    private boolean isRowBlank(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) return true;
        // Blank = no purchase quantity entered (col 2). Pre-filled templates always have cols 0/1 filled,
        // so we can't use "any column has content" as the signal anymore.
        return !StringUtils.hasText(cellText(row, 2, formatter, evaluator));
    }

    private boolean hasExcelExtension(String filename) {
        if (!StringUtils.hasText(filename)) return false;
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".xlsx") || lower.endsWith(".xls");
    }

    private String generateReceiptCode(LocalDateTime now, Integer actorId) {
        String baseCode = "GR" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + String.format("%04d", actorId == null ? 0 : actorId % 10000);
        String candidate = baseCode;
        int attempt = 1;
        while (goodsReceiptRepository.existsByReceiptCode(candidate)) candidate = baseCode + "-" + attempt++;
        return candidate;
    }

    private String buildImportNote(String receiptCode, ParsedImportRow row) {
        String conversion = row.purchaseQuantity().stripTrailingZeros().toPlainString() + " " + row.purchaseUnit().getSymbol()
                + " -> " + row.convertedQuantity().stripTrailingZeros().toPlainString() + " " + row.baseUnit().getSymbol();
        String prefix = "Import Excel phiếu nhập " + receiptCode + " (" + conversion + ")";
        return StringUtils.hasText(row.note()) ? prefix + " - " + row.note() : prefix;
    }

    private Branch resolveBranch(Integer branchId) {
        if (branchId == null) throw new NotFoundException("Branch not found");
        return branchRepository.findDetailById(branchId).orElseThrow(() -> new NotFoundException("Branch not found"));
    }

    private User resolveActor(Integer userId) {
        return userRepository.findEmployeeDetailById(userId).orElseThrow(() -> new NotFoundException("Current user not found"));
    }

    private void ensureCanImportBranch(Branch branch, CustomUserDetails currentUser) {
        if (currentUser == null) throw new ForbiddenException("You do not have permission to perform this action");
        if ("ADMIN".equalsIgnoreCase(currentUser.getRoleName())) return;
        User actor = resolveActor(currentUser.getUserId());
        Integer ownBranchId = actor.getProfile() != null && actor.getProfile().getBranch() != null ? actor.getProfile().getBranch().getId() : null;
        if (ownBranchId == null || !ownBranchId.equals(branch.getId())) throw new ForbiddenException("You do not have permission to import inventory for another branch");
    }

    private String cleanText(String value) { return StringUtils.hasText(value) ? value.trim().replaceAll("\\s+", " ") : null; }
    private String columnError(String column, String detail) { return "Cột '" + column + "': " + detail; }

    private String excelColumnName(int zeroBasedIndex) {
        int value = zeroBasedIndex + 1;
        StringBuilder name = new StringBuilder();
        while (value > 0) {
            int remainder = (value - 1) % 26;
            name.insert(0, (char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return name.toString();
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

    private BigDecimal quantity(BigDecimal value) { return value == null ? null : value.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal money(BigDecimal value) { return value == null ? null : value.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal rate(BigDecimal value) { return value == null ? null : value.setScale(6, RoundingMode.HALF_UP); }
    private BigDecimal rateOrNull(BigDecimal value) { return value == null ? null : value.setScale(6, RoundingMode.HALF_UP); }
    private BigDecimal defaultIfNull(BigDecimal value) { return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP); }
    private String normalizeHeader(String value) { return normalizeLookupText(value).replace("*", ""); }

    private String normalizeLookupText(String value) {
        if (!StringUtils.hasText(value)) return "";
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    private CellStyle createHeaderStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createTextStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle editableStyle(Workbook workbook, CellStyle base) {
        CellStyle s = workbook.createCellStyle();
        s.cloneStyleFrom(base);
        s.setLocked(false);
        return s;
    }

    private void createLockedCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        c.setCellStyle(style);
    }

    private void addUnitDropdown(Sheet sheet, int unitCount, int firstRow, int lastRow) {
        DataValidationHelper dvh = sheet.getDataValidationHelper();
        DataValidationConstraint dvc = dvh.createFormulaListConstraint("'Don vi'!$D$2:$D$" + (unitCount + 1));
        CellRangeAddressList range = new CellRangeAddressList(firstRow, lastRow, 3, 3);
        DataValidation dv = dvh.createValidation(dvc, range);
        dv.setShowErrorBox(true);
        dv.createErrorBox("Đơn vị không hợp lệ", "Vui lòng chọn từ danh sách sheet 'Don vi'.");
        sheet.addValidationData(dv);
    }

    private Integer readIngredientIdCell(Row row) {
        if (row == null) return null;
        Cell cell = row.getCell(HIDDEN_INGREDIENT_ID_COL);
        if (cell == null) return null;
        try {
            int id = (int) cell.getNumericCellValue();
            return id > 0 ? id : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeGuideSheet(Sheet sheet, Workbook workbook) {
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        CellStyle bodyStyle = workbook.createCellStyle();
        bodyStyle.setWrapText(true);
        String[] lines = {
                "Hướng dẫn import nhập kho GoldenHeart",
                "1. Đơn vị tồn kho là đơn vị công thức bếp sẽ trừ: g, kg, ml, l, pcs...",
                "2. Đơn vị mua là cách thực tế nhà cung cấp giao: hũ, bó, chai, thùng, bao...",
                "3. Nếu nhập Muối: tồn kho g, mua 10 hũ, quy đổi mỗi đơn vị 500, hệ thống cộng 5000 g.",
                "4. Nếu nhập Rau: tồn kho kg, mua 10 bó nhưng đã cân được tổng 3.8 kg, có thể bỏ trống Quy đổi mỗi đơn vị và nhập Tổng lượng quy đổi = 3.8.",
                "5. Đơn giá mua là giá cho 1 đơn vị mua. Hệ thống tự quy đổi ra giá vốn trên đơn vị tồn kho.",
                "6. Nếu một dòng sai, hệ thống không import bất kỳ dòng nào và sẽ chỉ rõ lỗi theo dòng."
        };
        for (int i = 0; i < lines.length; i++) {
            Row row = sheet.createRow(i);
            Cell cell = row.createCell(0);
            cell.setCellValue(lines[i]);
            cell.setCellStyle(i == 0 ? titleStyle : bodyStyle);
        }
        sheet.setColumnWidth(0, 18000);
    }

    private void writeUnitSheet(Sheet sheet, Workbook workbook, List<MeasurementUnit> units) {
        CellStyle headerStyle = createHeaderStyle(workbook, IndexedColors.GREY_25_PERCENT);
        Row header = sheet.createRow(0);
        String[] headers = {"ID", "Mã", "Tên đơn vị", "Ký hiệu"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, i == 2 ? 7000 : 3600);
        }
        for (int i = 0; i < units.size(); i++) {
            MeasurementUnit unit = units.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(unit.getId());
            row.createCell(1).setCellValue(unit.getCode());
            row.createCell(2).setCellValue(unit.getName());
            row.createCell(3).setCellValue(unit.getSymbol());
        }
    }

    private record ImportColumn(int index, String header, boolean required) {}
    private record DecimalCellValue(BigDecimal value, boolean present, String error) {}
    private record DateCellValue(LocalDate value, boolean present, String error) {}
    private record ConversionResult(BigDecimal conversionRate, BigDecimal convertedQuantity) {}
    private record ParsedImportFile(Branch branch, LocalDate receiptDate, String invoiceNumber, String note,
                                    InventoryImportPreviewResponse preview, List<ParsedImportRow> rows) {}
    private record ParsedImportRow(int rowNumber, boolean valid, String ingredientName, String baseUnitText,
                                   MeasurementUnit baseUnit, String purchaseUnitText, MeasurementUnit purchaseUnit,
                                   BigDecimal purchaseQuantity, BigDecimal conversionRate, BigDecimal convertedQuantity,
                                   BigDecimal purchaseUnitCost, BigDecimal baseUnitCost, BigDecimal lineTotal,
                                   BigDecimal minStockLevel, BigDecimal reorderLevel, LocalDate expiryDate,
                                   String batchNumber, String note, String action, Integer ingredientId, Integer inventoryId,
                                   BigDecimal currentQuantity, BigDecimal quantityAfterImport,
                                   BigDecimal averageUnitCostAfterImport, List<String> errors, List<String> warnings) {}

    private static class PreviewStockState {
        private final Integer ingredientId;
        private final Integer inventoryId;
        private final Integer unitId;
        private final int firstRowNumber;
        private final String firstActionLabel;
        private boolean consumed;
        private BigDecimal quantity;
        private BigDecimal minStockLevel;
        private BigDecimal reorderLevel;
        private BigDecimal averageUnitCost;
        private Integer defaultPurchaseUnitId;
        private BigDecimal defaultConversionRate;

        private PreviewStockState(Integer ingredientId, Integer inventoryId, Integer unitId, int firstRowNumber,
                                  String firstActionLabel, BigDecimal quantity, BigDecimal minStockLevel,
                                  BigDecimal reorderLevel, BigDecimal averageUnitCost,
                                  Integer defaultPurchaseUnitId, BigDecimal defaultConversionRate) {
            this.ingredientId = ingredientId;
            this.inventoryId = inventoryId;
            this.unitId = unitId;
            this.firstRowNumber = firstRowNumber;
            this.firstActionLabel = firstActionLabel;
            this.quantity = quantity;
            this.minStockLevel = minStockLevel;
            this.reorderLevel = reorderLevel;
            this.averageUnitCost = averageUnitCost;
            this.defaultPurchaseUnitId = defaultPurchaseUnitId;
            this.defaultConversionRate = defaultConversionRate;
        }

        static PreviewStockState newIngredient(MeasurementUnit unit, int firstRowNumber) {
            return new PreviewStockState(null, null, unit.getId(), firstRowNumber, "Tạo mới nguyên liệu", ZERO, ZERO, ZERO, ZERO, null, null);
        }

        static PreviewStockState existingIngredientWithoutInventory(Ingredient ingredient, MeasurementUnit unit, int firstRowNumber,
                                                                    Integer defaultPurchaseUnitId, BigDecimal defaultConversionRate) {
            return new PreviewStockState(ingredient.getId(), null, unit.getId(), firstRowNumber, "Tạo dòng tồn kho", ZERO, ZERO, ZERO, ZERO,
                    defaultPurchaseUnitId, defaultConversionRate);
        }

        static PreviewStockState existingInventory(Ingredient ingredient, Inventory inventory, MeasurementUnit unit, int firstRowNumber,
                                                   Integer defaultPurchaseUnitId, BigDecimal defaultConversionRate) {
            return new PreviewStockState(ingredient.getId(), inventory.getId(), unit.getId(), firstRowNumber, "Nhập thêm tồn kho",
                    defaultValue(inventory.getQuantity()), defaultValue(inventory.getMinStockLevel()),
                    defaultValue(inventory.getReorderLevel()), defaultValue(inventory.getAverageUnitCost()),
                    defaultPurchaseUnitId, defaultConversionRate);
        }

        String nextActionLabel() {
            if (!consumed) {
                consumed = true;
                return firstActionLabel;
            }
            return "Cộng cùng nguyên liệu trong file";
        }

        BigDecimal defaultRateFor(Integer purchaseUnitId) {
            return Objects.equals(defaultPurchaseUnitId, purchaseUnitId) ? defaultConversionRate : null;
        }

        void learnDefault(Integer purchaseUnitId, BigDecimal conversionRate) {
            if (defaultPurchaseUnitId == null && conversionRate != null) {
                defaultPurchaseUnitId = purchaseUnitId;
                defaultConversionRate = conversionRate;
            }
        }

        void applyRow(BigDecimal quantity, BigDecimal averageUnitCost, BigDecimal minStockLevel, BigDecimal reorderLevel) {
            this.quantity = quantity;
            this.averageUnitCost = averageUnitCost;
            this.minStockLevel = minStockLevel;
            this.reorderLevel = reorderLevel;
        }

        Integer ingredientId() { return ingredientId; }
        Integer inventoryId() { return inventoryId; }
        Integer unitId() { return unitId; }
        int firstRowNumber() { return firstRowNumber; }
        BigDecimal quantity() { return quantity; }
        BigDecimal minStockLevel() { return minStockLevel; }
        BigDecimal reorderLevel() { return reorderLevel; }
        BigDecimal averageUnitCost() { return averageUnitCost; }
        private static BigDecimal defaultValue(BigDecimal value) { return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP); }
    }
}
