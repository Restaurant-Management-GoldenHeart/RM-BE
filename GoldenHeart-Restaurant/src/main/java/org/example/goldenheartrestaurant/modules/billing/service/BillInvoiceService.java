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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class BillInvoiceService {

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
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
        Map<InvoiceLineItemKey, InvoiceLineItemAccumulator> groupedItems = new LinkedHashMap<>();

        orderItems.stream()
                .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
                .forEach(item -> {
                    BigDecimal unitPrice = nonNegative(item.getPrice());
                    String itemName = item.getMenuItem() != null ? item.getMenuItem().getName() : "Món ăn";
                    String note = normalizeNote(item.getNote());
                    InvoiceLineItemKey key = new InvoiceLineItemKey(
                            item.getMenuItem() != null ? item.getMenuItem().getId() : null,
                            itemName,
                            unitPrice,
                            note
                    );
                    groupedItems.computeIfAbsent(key, InvoiceLineItemAccumulator::new).add(item);
                });

        return groupedItems.values().stream()
                .map(InvoiceLineItemAccumulator::toView)
                .toList();
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
                    key.note()
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
            String note
    ) {
    }

    private record InvoicePaymentView(
            String method,
            String amount,
            String paidAt
    ) {
    }
}
