package org.example.goldenheartrestaurant.modules.paymentgateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
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

        PaymentGatewayTransaction activeTransaction = findLatestTransactionByBillId(billId);
        activeTransaction = expireIfNeeded(activeTransaction);
        if (activeTransaction != null && activeTransaction.getStatus() == PaymentGatewayTransactionStatus.PENDING) {
            throw new ConflictException("Bill already has an active payOS QR request");
        }

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

        PaymentGatewayTransaction latestTransaction = findLatestTransactionByBillId(billId);
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
        transaction = expireIfNeeded(transaction);
        return toResponse(transaction);
    }

    @Transactional
    public PayOsWebhookAckResponse processPayOsWebhook(PayOsWebhookRequest request) {
        ensurePayOsEnabled();

        if (request == null || request.data() == null) {
            throw new BadRequestException("Invalid payOS webhook payload");
        }
        if (!payOsSignatureService.isValidWebhookSignature(request.data(), request.signature())) {
            throw new BadRequestException("Invalid payOS webhook signature");
        }

        PaymentGatewayTransaction transaction = paymentGatewayTransactionRepository.findByProviderAndProviderOrderCodeForWebhook(
                        PaymentGatewayProvider.PAYOS,
                        request.data().orderCode()
                )
                .orElseThrow(() -> new NotFoundException("Payment gateway transaction not found"));

        transaction.setWebhookPayload(writePayloadSafely(request));
        transaction.setProviderReference(request.data().reference());
        transaction.setProviderCode(request.data().code());
        transaction.setProviderMessage(request.data().desc());
        if (StringUtils.hasText(request.data().paymentLinkId())) {
            transaction.setProviderPaymentLinkId(request.data().paymentLinkId());
        }

        if (transaction.getStatus() == PaymentGatewayTransactionStatus.PAID) {
            paymentGatewayTransactionRepository.save(transaction);
            return new PayOsWebhookAckResponse(true, "payOS webhook already processed");
        }

        if (!Boolean.TRUE.equals(request.success()) || !"00".equals(request.data().code())) {
            transaction.setStatus(PaymentGatewayTransactionStatus.FAILED);
            transaction.setFailureReason(request.data().desc());
            paymentGatewayTransactionRepository.save(transaction);
            return new PayOsWebhookAckResponse(true, "payOS webhook recorded as failed");
        }

        BigDecimal paidAmount = toBigDecimal(request.data().amount());
        if (transaction.getBill().getStatus() == BillStatus.PAID) {
            transaction.setStatus(PaymentGatewayTransactionStatus.FAILED);
            transaction.setFailureReason("Bill was already paid before this payOS callback was processed");
            transaction.setPaidAmount(paidAmount);
            paymentGatewayTransactionRepository.save(transaction);
            return new PayOsWebhookAckResponse(true, "Bill was already paid before payOS callback");
        }

        BigDecimal expectedAmount = calculateRemainingAmount(transaction.getBill());
        if (paidAmount.compareTo(expectedAmount) != 0) {
            transaction.setStatus(PaymentGatewayTransactionStatus.FAILED);
            transaction.setFailureReason("payOS amount does not match the current remaining bill amount");
            transaction.setPaidAmount(paidAmount);
            paymentGatewayTransactionRepository.save(transaction);
            return new PayOsWebhookAckResponse(true, "payOS amount mismatch recorded");
        }

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

    private PaymentGatewayTransaction expireIfNeeded(PaymentGatewayTransaction transaction) {
        if (transaction == null
                || transaction.getStatus() != PaymentGatewayTransactionStatus.PENDING
                || transaction.getExpiredAt() == null
                || !transaction.getExpiredAt().isBefore(LocalDateTime.now())) {
            return transaction;
        }

        transaction.setStatus(PaymentGatewayTransactionStatus.EXPIRED);
        transaction.setFailureReason("payOS QR request expired");
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
        // payOS should receive the same amount this bill is currently collecting.
        // A single aggregate line avoids mismatches once discounts, tax, or prior
        // payments make the remaining payable amount diverge from raw order items.
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
        BigDecimal paidAmount = bill.getPayments().stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingAmount = bill.getTotal().subtract(paidAmount);
        return remainingAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remainingAmount;
    }

    private BigDecimal toBigDecimal(Integer amount) {
        return amount == null ? BigDecimal.ZERO : BigDecimal.valueOf(amount.longValue());
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
