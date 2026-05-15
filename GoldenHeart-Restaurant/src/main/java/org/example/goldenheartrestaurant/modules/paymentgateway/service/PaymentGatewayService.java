package org.example.goldenheartrestaurant.modules.paymentgateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.goldenheartrestaurant.common.config.PayOsProperties;
import org.example.goldenheartrestaurant.common.exception.BadRequestException;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.billing.entity.Bill;
import org.example.goldenheartrestaurant.modules.billing.entity.BillStatus;
import org.example.goldenheartrestaurant.modules.billing.entity.Payment;
import org.example.goldenheartrestaurant.modules.billing.entity.PaymentMethod;
import org.example.goldenheartrestaurant.modules.billing.service.BillingService;
import org.example.goldenheartrestaurant.modules.customer.entity.Customer;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItem;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItemStatus;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos.PayOsCreatePaymentLinkRequest;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos.PayOsItemRequest;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos.PayOsPaymentLinkData;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos.PayOsResponse;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos.PayOsWebhookData;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos.PayOsWebhookRequest;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.request.CancelPayOsQrRequest;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.request.CreatePayOsQrRequest;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.response.PayOsWebhookAckResponse;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.response.PaymentGatewayTransactionResponse;
import org.example.goldenheartrestaurant.modules.paymentgateway.entity.PaymentGatewayProvider;
import org.example.goldenheartrestaurant.modules.paymentgateway.entity.PaymentGatewayTransaction;
import org.example.goldenheartrestaurant.modules.paymentgateway.entity.PaymentGatewayTransactionStatus;
import org.example.goldenheartrestaurant.modules.paymentgateway.repository.PaymentGatewayTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Service điều phối toàn bộ luồng payOS.
 *
 * Nguyên tắc quan trọng của lớp này:
 * - QR payOS chỉ đại diện cho một phiên thu tiền với provider
 * - tiền đã nhận thật chỉ được công nhận khi tạo {@link Payment} nội bộ thành công
 * - mọi lần đối soát với provider đều phải kiểm tra lại số tiền còn phải thu hiện tại
 *
 * Nói ngắn gọn:
 * transaction payOS là trạng thái bên ngoài,
 * còn {@link Payment} của billing mới là sự thật bên trong hệ thống.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayService {

    private static final DateTimeFormatter PAYOS_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PayOsProperties payOsProperties;
    private final PayOsClient payOsClient;
    private final PayOsSignatureService payOsSignatureService;
    private final PaymentGatewayTransactionRepository paymentGatewayTransactionRepository;
    private final BillingService billingService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentGatewayTransactionResponse createPayOsQr(Integer billId,
                                                           CreatePayOsQrRequest request,
                                                           CustomUserDetails currentUser) {
        ensurePayOsEnabled();

        Bill bill = billingService.getBillEntityById(billId, currentUser);
        if (bill.getStatus() == BillStatus.PAID) {
            throw new ConflictException("Bill is already fully paid");
        }

        // Một bill tại một thời điểm chỉ được phép tồn tại một QR đang chờ.
        // Nếu QR cũ đã hết hạn thì expire trước, nếu vẫn PENDING thì chặn tạo mới.
        PaymentGatewayTransaction activeTransaction = findLatestTransactionByBillId(billId);
        activeTransaction = expireIfNeeded(activeTransaction);
        if (activeTransaction != null && activeTransaction.getStatus() == PaymentGatewayTransactionStatus.PENDING) {
            throw new ConflictException("Bill already has an active payOS QR request");
        }

        // Số tiền gửi lên payOS phải đúng với phần còn lại của bill ngay lúc tạo QR.
        // Cách này tránh việc tổng order gốc lệch với tổng tiền thật sau discount, tax
        // hoặc sau một đợt thanh toán một phần trước đó.
        BigDecimal remainingAmount = calculateRemainingAmount(bill);
        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException("Bill has no remaining amount for payOS payment");
        }

        String returnUrl = StringUtils.hasText(request.returnUrl()) ? request.returnUrl().trim() : payOsProperties.getReturnUrl();
        String cancelUrl = StringUtils.hasText(request.cancelUrl()) ? request.cancelUrl().trim() : payOsProperties.getCancelUrl();
        long providerOrderCode = generateProviderOrderCode();
        int amount = toVndInteger(remainingAmount);
        String description = buildPayOsDescription(bill);
        long expiredAtEpoch = LocalDateTime.now()
                .plusMinutes(payOsProperties.getDefaultExpireMinutes())
                .atZone(ZoneId.systemDefault())
                .toEpochSecond();

        // Có webhook công khai thì cố gắng confirm trước để callback đến đúng endpoint.
        // Không confirm được vẫn cho tạo QR và để luồng polling/reconcile xử lý tiếp.
        confirmWebhookUrlIfConfigured();

        // Payload gửi sang payOS chỉ gồm một dòng tổng hợp.
        // Đây là cách an toàn nhất để amount bên provider khớp với remainingAmount hiện tại.
        PayOsCreatePaymentLinkRequest payOsRequest = new PayOsCreatePaymentLinkRequest(
                providerOrderCode,
                amount,
                description,
                extractBuyerName(bill.getOrder().getCustomer()),
                extractBuyerEmail(bill.getOrder().getCustomer()),
                extractBuyerPhone(bill.getOrder().getCustomer()),
                buildItems(bill, remainingAmount),
                cancelUrl,
                returnUrl,
                expiredAtEpoch,
                payOsSignatureService.createPaymentRequestSignature(amount, cancelUrl, description, providerOrderCode, returnUrl)
        );

        PayOsResponse<PayOsPaymentLinkData> payOsResponse = payOsClient.createPaymentLink(payOsRequest);
        PayOsPaymentLinkData payOsData = requirePayOsData(payOsResponse);

        // Lưu lại toàn bộ metadata cần cho POS:
        // - link checkout
        // - QR payload
        // - thời điểm hết hạn
        // - mã tham chiếu phía provider để polling, cancel và webhook đối soát.
        PaymentGatewayTransaction transaction = PaymentGatewayTransaction.builder()
                .bill(bill)
                .provider(PaymentGatewayProvider.PAYOS)
                .paymentMethod(PaymentMethod.PAYOS_QR)
                .providerOrderCode(providerOrderCode)
                .providerPaymentLinkId(payOsData.id())
                .requestedAmount(remainingAmount)
                .paidAmount(BigDecimal.ZERO)
                .status(PaymentGatewayTransactionStatus.PENDING)
                .checkoutUrl(payOsData.checkoutUrl())
                .qrCode(payOsData.qrCode())
                .deepLink(payOsData.deepLink())
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .expiredAt(resolveExpiredAt(payOsData.expiredAt(), expiredAtEpoch))
                .providerCode(payOsResponse.code())
                .providerMessage(payOsResponse.desc())
                .build();

        return toResponse(paymentGatewayTransactionRepository.save(transaction));
    }

    @Transactional
    public PaymentGatewayTransactionResponse getLatestPayOsQrByBillId(Integer billId, CustomUserDetails currentUser) {
        ensurePayOsEnabled();
        billingService.getBillEntityById(billId, currentUser);

        // Mỗi lần đọc QR hiện tại đều cho phép hệ thống tự động đối soát lại với provider
        // để frontend nhìn thấy trạng thái mới nhất mà không cần đợi webhook.
        PaymentGatewayTransaction latestTransaction = findLatestTransactionByBillId(billId);
        latestTransaction = syncTransactionWithProviderIfPending(latestTransaction);
        latestTransaction = expireIfNeeded(latestTransaction);
        return latestTransaction != null ? toResponse(latestTransaction) : null;
    }

    @Transactional
    public PaymentGatewayTransactionResponse cancelPayOsQr(Integer billId,
                                                           CancelPayOsQrRequest request,
                                                           CustomUserDetails currentUser) {
        ensurePayOsEnabled();
        billingService.getBillEntityById(billId, currentUser);

        PaymentGatewayTransaction transaction = findLatestTransactionByBillId(billId);
        transaction = expireIfNeeded(transaction);
        if (transaction == null || transaction.getStatus() != PaymentGatewayTransactionStatus.PENDING) {
            throw new ConflictException("Bill does not have any active payOS QR request");
        }

        PayOsResponse<PayOsPaymentLinkData> payOsResponse = payOsClient.cancelPaymentLink(
                resolveProviderIdentifier(transaction),
                normalizeCancelReason(request.reason())
        );

        transaction.setStatus(PaymentGatewayTransactionStatus.CANCELLED);
        transaction.setCancelledAt(LocalDateTime.now());
        transaction.setFailureReason(normalizeCancelReason(request.reason()));
        transaction.setProviderCode(payOsResponse.code());
        transaction.setProviderMessage(payOsResponse.desc());
        PayOsPaymentLinkData payOsData = payOsResponse.data();
        if (payOsData != null) {
            transaction.setProviderPaymentLinkId(payOsData.id());
        }

        return toResponse(paymentGatewayTransactionRepository.save(transaction));
    }

    @Transactional
    public PaymentGatewayTransactionResponse getTransactionById(Integer transactionId,
                                                                CustomUserDetails currentUser) {
        ensurePayOsEnabled();

        PaymentGatewayTransaction transaction = paymentGatewayTransactionRepository.findDetailById(transactionId)
                .orElseThrow(() -> new NotFoundException("Payment gateway transaction not found"));
        billingService.getBillEntityById(transaction.getBill().getId(), currentUser);
        transaction = syncTransactionWithProviderIfPending(transaction);
        transaction = expireIfNeeded(transaction);
        return toResponse(transaction);
    }

    @Transactional
    public PayOsWebhookAckResponse processPayOsWebhook(PayOsWebhookRequest request) {
        ensurePayOsEnabled();

        if (request == null || request.data() == null) {
            throw new BadRequestException("Invalid payOS webhook payload");
        }
        // Webhook chỉ hợp lệ khi chữ ký đúng.
        // Nếu bỏ qua bước này, bất kỳ ai biết endpoint đều có thể giả callback thành công.
        if (!payOsSignatureService.isValidWebhookSignature(request.data(), request.signature())) {
            throw new BadRequestException("Invalid payOS webhook signature");
        }

        PaymentGatewayTransaction transaction = paymentGatewayTransactionRepository.findByProviderAndProviderOrderCodeForWebhook(
                        PaymentGatewayProvider.PAYOS,
                        request.data().orderCode()
                )
                .orElse(null);

        // Trả ack sớm cho callback của giao dịch không nằm trong DB để payOS không retry vô hạn.
        if (transaction == null) {
            return new PayOsWebhookAckResponse(true, "payOS webhook accepted for an untracked transaction");
        }

        // Lưu lại payload và metadata provider trước, kể cả khi callback này về trạng thái fail.
        // Việc này giúp debug và đối soát lại dữ liệu sau này.
        transaction.setWebhookPayload(writePayloadSafely(request));
        transaction.setProviderReference(request.data().reference());
        transaction.setProviderCode(request.data().code());
        transaction.setProviderMessage(request.data().desc());
        if (StringUtils.hasText(request.data().paymentLinkId())) {
            transaction.setProviderPaymentLinkId(request.data().paymentLinkId());
        }

        // Nếu đã xử lý thành công trước đó thì webhook lặp lại phải trở thành idempotent.
        if (transaction.getStatus() == PaymentGatewayTransactionStatus.PAID) {
            paymentGatewayTransactionRepository.save(transaction);
            return new PayOsWebhookAckResponse(true, "payOS webhook already processed");
        }

        // payOS báo thất bại thì chỉ ghi nhận vào transaction, không được tự ý tạo Payment nội bộ.
        if (!Boolean.TRUE.equals(request.success()) || !"00".equals(request.data().code())) {
            transaction.setStatus(PaymentGatewayTransactionStatus.FAILED);
            transaction.setFailureReason(request.data().desc());
            paymentGatewayTransactionRepository.save(transaction);
            return new PayOsWebhookAckResponse(true, "payOS webhook recorded as failed");
        }

        BigDecimal paidAmount = toBigDecimal(request.data().amount());
        // Nếu bill đã PAID bằng một kênh khác trước khi webhook này về,
        // callback phải được đánh dấu failed để tránh nhận đôi tiền.
        if (transaction.getBill().getStatus() == BillStatus.PAID) {
            transaction.setStatus(PaymentGatewayTransactionStatus.FAILED);
            transaction.setFailureReason("Bill was already paid before this payOS callback was processed");
            transaction.setPaidAmount(paidAmount);
            paymentGatewayTransactionRepository.save(transaction);
            return new PayOsWebhookAckResponse(true, "Bill was already paid before payOS callback");
        }

        // Số tiền callback phải khớp với số tiền còn phải thu tại thời điểm xử lý.
        // Nếu không khớp, hệ thống chỉ đánh dấu lỗi để nhân viên/quản lý đối soát tay.
        BigDecimal expectedAmount = calculateRemainingAmount(transaction.getBill());
        if (paidAmount.compareTo(expectedAmount) != 0) {
            transaction.setStatus(PaymentGatewayTransactionStatus.FAILED);
            transaction.setFailureReason("payOS amount does not match the current remaining bill amount");
            transaction.setPaidAmount(paidAmount);
            paymentGatewayTransactionRepository.save(transaction);
            return new PayOsWebhookAckResponse(true, "payOS amount mismatch recorded");
        }

        // Đây mới là điểm biến thanh toán provider thành Payment nội bộ của hệ thống.
        Payment payment = billingService.recordConfirmedGatewayPayment(
                transaction.getBill().getId(),
                paidAmount,
                PaymentMethod.PAYOS_QR,
                parsePayOsPaidAt(request.data().transactionDateTime())
        );

        transaction.setPayment(payment);
        transaction.setPaidAmount(paidAmount);
        transaction.setStatus(PaymentGatewayTransactionStatus.PAID);
        transaction.setConfirmedAt(payment.getPaidAt());
        paymentGatewayTransactionRepository.save(transaction);

        return new PayOsWebhookAckResponse(true, "payOS webhook processed successfully");
    }

    private void ensurePayOsEnabled() {
        if (!payOsProperties.isEnabled()) {
            throw new ConflictException("payOS integration is disabled");
        }
        if (!StringUtils.hasText(payOsProperties.getClientId())
                || !StringUtils.hasText(payOsProperties.getApiKey())
                || !StringUtils.hasText(payOsProperties.getChecksumKey())) {
            throw new ConflictException("payOS credentials are not configured");
        }
    }

    private PaymentGatewayTransaction findLatestTransactionByBillId(Integer billId) {
        return paymentGatewayTransactionRepository.findAllByBillIdAndProviderOrderByIdDesc(billId, PaymentGatewayProvider.PAYOS)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void confirmWebhookUrlIfConfigured() {
        if (!StringUtils.hasText(payOsProperties.getWebhookUrl())) {
            return;
        }

        String webhookUrl = payOsProperties.getWebhookUrl().trim();

        try {
            PayOsResponse<java.util.Map<String, Object>> response = payOsClient.confirmWebhook(webhookUrl);
            if (response == null || !"00".equals(response.code())) {
                log.warn("[PAYOS] Webhook URL confirmation returned non-success for {}. QR creation will continue and rely on polling/reconcile.", webhookUrl);
            }
        } catch (ConflictException exception) {
            log.warn("[PAYOS] Webhook URL confirmation failed for {}. QR creation will continue and rely on polling/reconcile. Cause: {}", webhookUrl, exception.getMessage());
        }
    }

    private PaymentGatewayTransaction syncTransactionWithProviderIfPending(PaymentGatewayTransaction transaction) {
        if (transaction == null || transaction.getStatus() != PaymentGatewayTransactionStatus.PENDING) {
            return transaction;
        }

        // Polling lấy dữ liệu mới nhất từ provider để POS có thể:
        // - thấy QR đã bị huỷ
        // - thấy link đã hết hạn
        // - thấy provider đã thu tiền dù webhook chưa kịp về
        PayOsResponse<PayOsPaymentLinkData> payOsResponse = payOsClient.getPaymentLink(resolveProviderIdentifier(transaction));
        PayOsPaymentLinkData payOsData = requirePayOsData(payOsResponse);

        transaction.setProviderCode(payOsResponse.code());
        transaction.setProviderMessage(payOsResponse.desc());
        if (StringUtils.hasText(payOsData.id())) {
            transaction.setProviderPaymentLinkId(payOsData.id());
        }
        if (StringUtils.hasText(payOsData.checkoutUrl())) {
            transaction.setCheckoutUrl(payOsData.checkoutUrl());
        }
        if (StringUtils.hasText(payOsData.qrCode())) {
            transaction.setQrCode(payOsData.qrCode());
        }
        if (StringUtils.hasText(payOsData.deepLink())) {
            transaction.setDeepLink(payOsData.deepLink());
        }
        if (payOsData.expiredAt() != null) {
            transaction.setExpiredAt(resolveExpiredAt(payOsData.expiredAt(), payOsData.expiredAt()));
        }

        BigDecimal providerPaidAmount = resolveProviderPaidAmount(payOsData, transaction);
        if (providerPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            transaction.setPaidAmount(providerPaidAmount);
        }

        String providerStatus = normalizeProviderStatus(payOsData.status());
        // Có 2 dấu hiệu đủ để xem như đã thu tiền:
        // - provider trả status PAID
        // - hoặc số tiền đã thu >= requestedAmount
        if ("PAID".equals(providerStatus) || providerPaidAmount.compareTo(transaction.getRequestedAmount()) >= 0) {
            return settleConfirmedProviderTransaction(transaction, providerPaidAmount);
        }
        if ("CANCELLED".equals(providerStatus)) {
            transaction.setStatus(PaymentGatewayTransactionStatus.CANCELLED);
            transaction.setCancelledAt(LocalDateTime.now());
            transaction.setFailureReason("payOS payment link was cancelled");
            return paymentGatewayTransactionRepository.save(transaction);
        }
        if ("EXPIRED".equals(providerStatus)) {
            transaction.setStatus(PaymentGatewayTransactionStatus.EXPIRED);
            transaction.setFailureReason("payOS payment link expired on provider");
            return paymentGatewayTransactionRepository.save(transaction);
        }

        return paymentGatewayTransactionRepository.save(transaction);
    }

    private PaymentGatewayTransaction expireIfNeeded(PaymentGatewayTransaction transaction) {
        if (transaction == null
                || transaction.getStatus() != PaymentGatewayTransactionStatus.PENDING
                || transaction.getExpiredAt() == null
                || !transaction.getExpiredAt().isBefore(LocalDateTime.now())) {
            return transaction;
        }

        // Hệ thống tự đánh dấu hết hạn để frontend không tiếp tục coi QR cũ là còn dùng được.
        transaction.setStatus(PaymentGatewayTransactionStatus.EXPIRED);
        transaction.setFailureReason("payOS QR request expired");
        return paymentGatewayTransactionRepository.save(transaction);
    }

    private PaymentGatewayTransaction settleConfirmedProviderTransaction(PaymentGatewayTransaction transaction,
                                                                         BigDecimal paidAmount) {
        // Provider có lúc không trả amountPaid rõ ràng trong mọi API đọc.
        // Khi đó fallback về requestedAmount để xử lý tiếp theo cùng một công thức.
        BigDecimal effectivePaidAmount = paidAmount.compareTo(BigDecimal.ZERO) > 0 ? paidAmount : transaction.getRequestedAmount();
        if (transaction.getBill().getStatus() == BillStatus.PAID) {
            transaction.setStatus(PaymentGatewayTransactionStatus.PAID);
            transaction.setPaidAmount(effectivePaidAmount);
            if (transaction.getConfirmedAt() == null) {
                transaction.setConfirmedAt(LocalDateTime.now());
            }
            return paymentGatewayTransactionRepository.save(transaction);
        }

        // Với polling reconcile, vẫn phải lặp lại rule amount-match y như webhook.
        BigDecimal expectedAmount = calculateRemainingAmount(transaction.getBill());
        if (effectivePaidAmount.compareTo(expectedAmount) != 0) {
            transaction.setStatus(PaymentGatewayTransactionStatus.FAILED);
            transaction.setFailureReason("payOS amount does not match the current remaining bill amount");
            transaction.setPaidAmount(effectivePaidAmount);
            return paymentGatewayTransactionRepository.save(transaction);
        }

        Payment payment = billingService.recordConfirmedGatewayPayment(
                transaction.getBill().getId(),
                effectivePaidAmount,
                PaymentMethod.PAYOS_QR,
                LocalDateTime.now()
        );

        transaction.setPayment(payment);
        transaction.setPaidAmount(effectivePaidAmount);
        transaction.setStatus(PaymentGatewayTransactionStatus.PAID);
        transaction.setConfirmedAt(payment.getPaidAt());
        transaction.setFailureReason(null);
        return paymentGatewayTransactionRepository.save(transaction);
    }

    private long generateProviderOrderCode() {
        long candidate = System.currentTimeMillis();
        while (paymentGatewayTransactionRepository.existsByProviderAndProviderOrderCode(PaymentGatewayProvider.PAYOS, candidate)) {
            candidate++;
        }
        return candidate;
    }

    private int toVndInteger(BigDecimal value) {
        try {
            return value.setScale(0, RoundingMode.HALF_UP).intValueExact();
        } catch (ArithmeticException exception) {
            throw new ConflictException("payOS only supports integer VND amounts");
        }
    }

    private String buildPayOsDescription(Bill bill) {
        return "GHB" + bill.getId();
    }

    private String extractBuyerName(Customer customer) {
        return customer != null && StringUtils.hasText(customer.getName()) ? customer.getName().trim() : null;
    }

    private String extractBuyerEmail(Customer customer) {
        return customer != null && StringUtils.hasText(customer.getEmail()) ? customer.getEmail().trim() : null;
    }

    private String extractBuyerPhone(Customer customer) {
        return customer != null && StringUtils.hasText(customer.getPhone()) ? customer.getPhone().trim() : null;
    }

    private List<PayOsItemRequest> buildItems(Bill bill) {
        return bill.getOrder().getOrderItems().stream()
                .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
                .map(this::toPayOsItem)
                .toList();
    }

    private List<PayOsItemRequest> buildItems(Bill bill, BigDecimal remainingAmount) {
        // Gửi một dòng tổng hợp thay vì từng món.
        // Lý do:
        // - tổng tiền bill có thể đã thay đổi bởi tax, discount, loyalty
        // - bill có thể đã thu một phần trước đó
        // - provider chỉ cần biết số tiền phiên này cần thu, không cần hiểu chi tiết từng món
        return List.of(
                new PayOsItemRequest(
                        buildAggregateItemName(bill),
                        1,
                        toVndInteger(remainingAmount),
                        "bill"
                )
        );
    }

    private PayOsItemRequest toPayOsItem(OrderItem item) {
        return new PayOsItemRequest(
                item.getMenuItem().getName(),
                item.getQuantity(),
                toVndInteger(item.getPrice()),
                "phần"
        );
    }

    private String buildAggregateItemName(Bill bill) {
        return "Thanh toan bill #" + bill.getId();
    }

    private LocalDateTime resolveExpiredAt(Long payOsExpiredAt, long fallbackExpiredAt) {
        long epochSeconds = payOsExpiredAt != null ? payOsExpiredAt : fallbackExpiredAt;
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
    }

    private PayOsPaymentLinkData requirePayOsData(PayOsResponse<PayOsPaymentLinkData> response) {
        if (response == null || response.data() == null) {
            throw new ConflictException("payOS did not return any payment link data");
        }
        return response.data();
    }

    private Object resolveProviderIdentifier(PaymentGatewayTransaction transaction) {
        return StringUtils.hasText(transaction.getProviderPaymentLinkId())
                ? transaction.getProviderPaymentLinkId()
                : transaction.getProviderOrderCode();
    }

    private String normalizeCancelReason(String reason) {
        return StringUtils.hasText(reason) ? reason.trim() : "Cancelled by staff";
    }

    private LocalDateTime parsePayOsPaidAt(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(rawValue.trim(), PAYOS_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(rawValue.trim());
            } catch (DateTimeParseException exception) {
                return LocalDateTime.now();
            }
        }
    }

    private BigDecimal calculateRemainingAmount(Bill bill) {
        // Tính trực tiếp trên bill đang load để luồng payment gateway
        // luôn dựa trên số liệu hiện tại nhất của backend.
        BigDecimal paidAmount = bill.getPayments().stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingAmount = bill.getTotal().subtract(paidAmount);
        return remainingAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remainingAmount;
    }

    private BigDecimal toBigDecimal(Integer amount) {
        return amount == null ? BigDecimal.ZERO : BigDecimal.valueOf(amount.longValue());
    }

    private BigDecimal resolveProviderPaidAmount(PayOsPaymentLinkData payOsData, PaymentGatewayTransaction transaction) {
        BigDecimal paidAmount = toBigDecimal(payOsData.amountPaid());
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            return paidAmount;
        }
        if ("PAID".equals(normalizeProviderStatus(payOsData.status()))) {
            return payOsData.amount() != null ? toBigDecimal(payOsData.amount()) : transaction.getRequestedAmount();
        }
        return BigDecimal.ZERO;
    }

    private String normalizeProviderStatus(String rawStatus) {
        return StringUtils.hasText(rawStatus) ? rawStatus.trim().toUpperCase() : "";
    }

    private String writePayloadSafely(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return "{\"message\":\"Cannot serialize payOS webhook payload\"}";
        }
    }

    private PaymentGatewayTransactionResponse toResponse(PaymentGatewayTransaction transaction) {
        return new PaymentGatewayTransactionResponse(
                transaction.getId(),
                transaction.getBill().getId(),
                transaction.getBill().getOrder().getId(),
                transaction.getBill().getOrder().getTable() != null ? transaction.getBill().getOrder().getTable().getId() : null,
                transaction.getBill().getOrder().getTable() != null ? transaction.getBill().getOrder().getTable().getTableNumber() : null,
                transaction.getProvider().name(),
                transaction.getPaymentMethod().name(),
                transaction.getProviderOrderCode(),
                transaction.getProviderPaymentLinkId(),
                transaction.getRequestedAmount(),
                transaction.getPaidAmount(),
                transaction.getStatus().name(),
                transaction.getCheckoutUrl(),
                transaction.getQrCode(),
                transaction.getDeepLink(),
                transaction.getExpiredAt(),
                transaction.getConfirmedAt(),
                transaction.getCancelledAt(),
                transaction.getFailureReason(),
                transaction.getProviderReference(),
                transaction.getProviderCode(),
                transaction.getProviderMessage(),
                transaction.getPayment() != null ? transaction.getPayment().getId() : null,
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
