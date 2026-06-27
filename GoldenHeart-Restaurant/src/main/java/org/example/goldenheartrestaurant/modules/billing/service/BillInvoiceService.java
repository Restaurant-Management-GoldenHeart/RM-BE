package org.example.goldenheartrestaurant.modules.billing.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.billing.entity.Bill;
import org.example.goldenheartrestaurant.modules.billing.entity.BillStatus;
import org.example.goldenheartrestaurant.modules.billing.entity.Payment;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItem;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItemStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BillInvoiceService {

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    // Note của combo item: "[Combo:N] <tên>" (N = số lượng combo) hoặc "[Combo] <tên>" (format cũ)
    private static final Pattern COMBO_NOTE_PATTERN = Pattern.compile("^\\[Combo(?::(\\d+))?\\]\\s+(.+)$");
    private static final List<String> FALLBACK_FONT_PATHS = List.of(
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/tahoma.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf"
    );

    private final BillingService billingService;
    private final TemplateEngine templateEngine;

    @Value("${app.invoice.pdf-font-path:}")
    private String configuredFontPath;

    public BillInvoiceService(
            BillingService billingService,
            @Qualifier("invoicePdfTemplateEngine") TemplateEngine templateEngine
    ) {
        this.billingService = billingService;
        this.templateEngine = templateEngine;
    }

    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Integer billId, CustomUserDetails currentUser) {
        Bill bill = billingService.getBillEntityById(billId, currentUser);
        if (!isInvoiceReady(bill)) {
            throw new ConflictException("Invoice PDF is only available after the bill is fully paid");
        }

        Context context = new Context(VIETNAMESE);
        context.setVariable("invoice", buildView(bill));

        String html = templateEngine.process("billing/bill-invoice", context);
        return renderPdf(html);
    }

    public String buildInvoiceFilename(Integer billId) {
        return "hoa-don-" + billId + ".pdf";
    }

    private InvoiceView buildView(Bill bill) {
        String restaurantName = bill.getOrder().getBranch().getRestaurant() != null
                && StringUtils.hasText(bill.getOrder().getBranch().getRestaurant().getName())
                ? bill.getOrder().getBranch().getRestaurant().getName().trim()
                : "GoldenHeart Restaurant";

        List<InvoiceLineItemView> items = toGroupedLineItemViews(bill.getOrder().getOrderItems());

        List<InvoicePaymentView> payments = bill.getPayments().stream()
                .sorted(Comparator.comparing(Payment::getPaidAt, Comparator.nullsLast(LocalDateTime::compareTo)))
                .map(this::toPaymentView)
                .toList();

        LocalDateTime lastPaidAt = bill.getPayments().stream()
                .map(Payment::getPaidAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        BigDecimal subtotal = nonNegative(bill.getSubtotal());
        BigDecimal tax = nonNegative(bill.getTax());
        BigDecimal totalDiscount = nonNegative(bill.getDiscount());
        BigDecimal loyaltyDiscount = nonNegative(bill.getLoyaltyDiscount());
        BigDecimal manualDiscount = totalDiscount.subtract(loyaltyDiscount);
        if (manualDiscount.compareTo(BigDecimal.ZERO) < 0) {
            manualDiscount = BigDecimal.ZERO;
        }

        BigDecimal paidAmount = bill.getPayments().stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingAmount = nonNegative(bill.getTotal()).subtract(paidAmount);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        return new InvoiceView(
                restaurantName,
                blankFallback(bill.getOrder().getBranch().getName(), "Chi nhánh chưa đặt tên"),
                blankFallback(bill.getOrder().getBranch().getAddress(), "Chưa cập nhật địa chỉ"),
                blankFallback(bill.getOrder().getBranch().getPhone(), "--"),
                bill.getId(),
                bill.getOrder().getId(),
                bill.getOrder().getTable() != null ? bill.getOrder().getTable().getTableNumber() : "Mang về",
                blankFallback(bill.getOrder().getCustomer() != null ? bill.getOrder().getCustomer().getName() : null, "Khách lẻ"),
                resolveDisplayName(bill.getOrder().getCreatedBy()),
                resolveDisplayName(bill.getCreatedBy() != null ? bill.getCreatedBy() : bill.getOrder().getCreatedBy()),
                formatDateTime(bill.getOrder().getCreatedAt()),
                formatDateTime(lastPaidAt),
                items,
                payments,
                payments.stream().map(InvoicePaymentView::method).distinct().toList(),
                buildVatLabel(subtotal, tax),
                buildMemberLabel(bill.getAppliedCustomerTier() != null ? bill.getAppliedCustomerTier().getDiscountRate() : BigDecimal.ZERO),
                formatCurrency(subtotal),
                formatCurrency(tax),
                manualDiscount.compareTo(BigDecimal.ZERO) > 0,
                formatCurrency(manualDiscount),
                loyaltyDiscount.compareTo(BigDecimal.ZERO) > 0,
                formatCurrency(loyaltyDiscount),
                formatCurrency(nonNegative(bill.getTotal())),
                formatCurrency(paidAmount),
                formatCurrency(remainingAmount)
        );
    }

    private List<InvoiceLineItemView> toGroupedLineItemViews(List<OrderItem> orderItems) {
        // Tách combo items (note = "[Combo:N] <tên>" hoặc "[Combo] <tên>") và regular items
        Map<String, List<OrderItem>> comboGroups = new LinkedHashMap<>();
        Map<String, Integer> comboQuantities = new LinkedHashMap<>(); // comboName → số lượng combo
        List<OrderItem> regularItems = new ArrayList<>();

        orderItems.stream()
                .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
                .forEach(item -> {
                    String note = normalizeNote(item.getNote());
                    if (note != null) {
                        Matcher m = COMBO_NOTE_PATTERN.matcher(note);
                        if (m.matches()) {
                            String qtyStr = m.group(1);   // nullable — format cũ không có
                            String comboName = m.group(2);
                            if (qtyStr != null) {
                                comboQuantities.putIfAbsent(comboName, Integer.parseInt(qtyStr));
                            }
                            comboGroups.computeIfAbsent(comboName, k -> new ArrayList<>()).add(item);
                            return;
                        }
                    }
                    regularItems.add(item);
                });

        List<InvoiceLineItemView> result = new ArrayList<>();

        // Combo groups: header row + indented sub-items
        comboGroups.forEach((comboName, items) -> {
            BigDecimal groupTotal = items.stream()
                    .map(i -> nonNegative(i.getPrice())
                            .multiply(BigDecimal.valueOf(i.getQuantity() != null ? i.getQuantity() : 0)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Tính SL combo:
            // Khi gộp bàn, cùng combo từ nhiều bàn tạo ra nhiều "batch" — mỗi batch có N combo.
            // Phát hiện số batch bằng cách đếm số lần xuất hiện của từng sub-item trong group.
            // (Mỗi sub-item xuất hiện đúng 1 lần per batch → maxOccurrences = batchCount)
            Integer nPerBatch = comboQuantities.get(comboName); // N từ note, per batch
            Integer comboQty = null;
            String comboUnitPriceStr = null;
            if (nPerBatch != null && nPerBatch > 0) {
                long batchCount = items.stream()
                        .collect(Collectors.groupingBy(
                                i -> i.getMenuItem() != null ? i.getMenuItem().getId() : -1,
                                Collectors.counting()))
                        .values().stream()
                        .mapToLong(Long::longValue).max().orElse(1L);
                comboQty = (int) (nPerBatch * batchCount);
                BigDecimal unitPrice = groupTotal.divide(BigDecimal.valueOf(comboQty), 0, java.math.RoundingMode.HALF_UP);
                comboUnitPriceStr = formatCurrency(unitPrice);
            }

            result.add(new InvoiceLineItemView(comboName, comboQty, comboUnitPriceStr, formatCurrency(groupTotal), null, "COMBO_HEADER"));

            // Group sub-items cùng menuItem
            Map<InvoiceLineItemKey, InvoiceLineItemAccumulator> subGrouped = new LinkedHashMap<>();
            items.forEach(i -> {
                BigDecimal unitPrice = nonNegative(i.getPrice());
                String itemName = i.getMenuItem() != null ? i.getMenuItem().getName() : "Món ăn";
                subGrouped.computeIfAbsent(
                        new InvoiceLineItemKey(i.getMenuItem() != null ? i.getMenuItem().getId() : null, itemName, unitPrice, null),
                        InvoiceLineItemAccumulator::new
                ).add(i);
            });
            subGrouped.values().forEach(acc -> {
                InvoiceLineItemView v = acc.toView();
                result.add(new InvoiceLineItemView(v.name(), v.quantity(), v.unitPrice(), v.lineTotal(), null, "COMBO_ITEM"));
            });
        });

        // Regular items
        Map<InvoiceLineItemKey, InvoiceLineItemAccumulator> regularGrouped = new LinkedHashMap<>();
        regularItems.forEach(item -> {
            BigDecimal unitPrice = nonNegative(item.getPrice());
            String itemName = item.getMenuItem() != null ? item.getMenuItem().getName() : "Món ăn";
            String note = normalizeNote(item.getNote());
            regularGrouped.computeIfAbsent(
                    new InvoiceLineItemKey(item.getMenuItem() != null ? item.getMenuItem().getId() : null, itemName, unitPrice, note),
                    InvoiceLineItemAccumulator::new
            ).add(item);
        });
        regularGrouped.values().stream().map(InvoiceLineItemAccumulator::toView).forEach(result::add);

        return result;
    }

    private InvoicePaymentView toPaymentView(Payment payment) {
        return new InvoicePaymentView(
                payment.getMethod() != null ? payment.getMethod().name() : "UNKNOWN",
                formatCurrency(nonNegative(payment.getAmount())),
                formatDateTime(payment.getPaidAt())
        );
    }

    private byte[] renderPdf(String html) {
        Path fontPath = resolveFontPath();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(fontPath.toFile(), "InvoiceSans");
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            log.error("Invoice PDF generation failed. fontPath={}, htmlPreview={}",
                    fontPath,
                    html.substring(0, Math.min(html.length(), 400)),
                    exception);
            throw new IllegalStateException("Cannot generate invoice PDF", exception);
        }
    }

    private Path resolveFontPath() {
        if (StringUtils.hasText(configuredFontPath)) {
            try {
                Path configuredPath = Path.of(configuredFontPath.trim());
                if (Files.isRegularFile(configuredPath)) {
                    return configuredPath;
                }
            } catch (InvalidPathException exception) {
                log.warn("Ignore invalid app.invoice.pdf-font-path value: {}", configuredFontPath);
            }
        }

        return FALLBACK_FONT_PATHS.stream()
                .map(Path::of)
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No Unicode font found for invoice PDF. Configure app.invoice.pdf-font-path"
                ));
    }

    private String resolveDisplayName(User user) {
        if (user == null) {
            return "Không rõ";
        }
        if (user.getProfile() != null && StringUtils.hasText(user.getProfile().getFullName())) {
            return user.getProfile().getFullName().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return "Không rõ";
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value != null ? value.max(BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    private boolean isInvoiceReady(Bill bill) {
        if (bill.getStatus() == BillStatus.PAID) {
            return true;
        }

        BigDecimal total = nonNegative(bill.getTotal());
        BigDecimal paidAmount = bill.getPayments().stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return paidAmount.compareTo(BigDecimal.ZERO) > 0 && paidAmount.compareTo(total) >= 0;
    }

    private String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(VIETNAMESE).format(nonNegative(value));
    }

    private String buildVatLabel(BigDecimal subtotal, BigDecimal tax) {
        BigDecimal safeSubtotal = nonNegative(subtotal);
        BigDecimal safeTax = nonNegative(tax);
        if (safeSubtotal.compareTo(BigDecimal.ZERO) <= 0 || safeTax.compareTo(BigDecimal.ZERO) <= 0) {
            return "VAT (0%)";
        }

        BigDecimal rate = safeTax.multiply(BigDecimal.valueOf(100))
                .divide(safeSubtotal, 2, java.math.RoundingMode.HALF_UP);
        return "VAT (" + formatRate(rate) + ")";
    }

    private String buildMemberLabel(BigDecimal rate) {
        return "Member (" + formatRate(rate) + ")";
    }

    private String formatRate(BigDecimal rate) {
        BigDecimal safeRate = rate != null ? rate.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal stripped = safeRate.stripTrailingZeros();
        return stripped.toPlainString() + "%";
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "--:--";
        }
        return DATE_TIME_FORMATTER.format(value);
    }

    private String blankFallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String normalizeNote(String note) {
        return StringUtils.hasText(note) ? note.trim() : null;
    }

    private record InvoiceLineItemKey(
            Integer menuItemId,
            String itemName,
            BigDecimal unitPrice,
            String note
    ) {
    }

    private class InvoiceLineItemAccumulator {
        private final InvoiceLineItemKey key;
        private int quantity;

        private InvoiceLineItemAccumulator(InvoiceLineItemKey key) {
            this.key = key;
        }

        private void add(OrderItem item) {
            quantity += item.getQuantity() != null ? item.getQuantity() : 0;
        }

        private InvoiceLineItemView toView() {
            BigDecimal lineTotal = key.unitPrice().multiply(BigDecimal.valueOf(quantity));

            return new InvoiceLineItemView(
                    key.itemName(),
                    quantity,
                    formatCurrency(key.unitPrice()),
                    formatCurrency(lineTotal),
                    key.note(),
                    "REGULAR"
            );
        }
    }

    private record InvoiceView(
            String restaurantName,
            String branchName,
            String branchAddress,
            String branchPhone,
            Integer billId,
            Integer orderId,
            String tableName,
            String customerName,
            String openedByName,
            String billCreatedByName,
            String openedAt,
            String paidAt,
            List<InvoiceLineItemView> items,
            List<InvoicePaymentView> payments,
            List<String> paymentMethods,
            String vatLabel,
            String memberLabel,
            String subtotal,
            String tax,
            boolean hasManualDiscount,
            String manualDiscount,
            boolean hasMemberDiscount,
            String loyaltyDiscount,
            String total,
            String paidAmount,
            String remainingAmount
    ) {
    }

    private record InvoiceLineItemView(
            String name,
            Integer quantity,
            String unitPrice,
            String lineTotal,
            String note,
            String rowType   // "REGULAR", "COMBO_HEADER", "COMBO_ITEM"
    ) {
    }

    private record InvoicePaymentView(
            String method,
            String amount,
            String paidAt
    ) {
    }
}
