package org.example.goldenheartrestaurant.modules.billing.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.BadRequestException;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.billing.dto.request.CreateBillRequest;
import org.example.goldenheartrestaurant.modules.billing.dto.request.CreatePaymentRequest;
import org.example.goldenheartrestaurant.modules.billing.dto.response.BillHistoryItemResponse;
import org.example.goldenheartrestaurant.modules.billing.dto.response.BillResponse;
import org.example.goldenheartrestaurant.modules.billing.dto.response.CheckoutPreviewResponse;
import org.example.goldenheartrestaurant.modules.billing.dto.response.PaymentResponse;
import org.example.goldenheartrestaurant.modules.billing.entity.Bill;
import org.example.goldenheartrestaurant.modules.billing.entity.BillStatus;
import org.example.goldenheartrestaurant.modules.billing.entity.Payment;
import org.example.goldenheartrestaurant.modules.billing.entity.PaymentMethod;
import org.example.goldenheartrestaurant.modules.billing.repository.BillRepository;
import org.example.goldenheartrestaurant.modules.billing.repository.PaymentRepository;
import org.example.goldenheartrestaurant.modules.customer.entity.Customer;
import org.example.goldenheartrestaurant.modules.customer.entity.CustomerLoyaltyTransaction;
import org.example.goldenheartrestaurant.modules.customer.entity.CustomerTier;
import org.example.goldenheartrestaurant.modules.customer.service.CustomerLoyaltyService;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.StockMovementRepository;
import org.example.goldenheartrestaurant.modules.order.entity.Order;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItem;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItemStatus;
import org.example.goldenheartrestaurant.modules.order.entity.OrderStatus;
import org.example.goldenheartrestaurant.modules.order.service.OrderManagementService;
import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTable;
import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTableStatus;
import org.example.goldenheartrestaurant.modules.restaurant.repository.RestaurantTableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service xử lý toàn bộ nghiệp vụ thanh toán cuối cùng của order.
 *
 * Trách nhiệm chính:
 * - preview hóa đơn trước khi chốt
 * - tạo hoặc cập nhật bill
 * - nhận thanh toán nhiều lần cho cùng một bill
 * - đồng bộ trạng thái bill
 * - đóng order khi bill đã thanh toán đủ
 * - đưa bàn sang CLEANING
 * - cộng điểm loyalty sau khi thanh toán xong
 *
 * Đây là lớp rất quan trọng vì nó là điểm giao nhau của:
 * - order
 * - table
 * - payment
 * - loyalty
 * - báo cáo doanh thu/lợi nhuận
 */
@Service
@RequiredArgsConstructor
public class BillingService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_STAFF = "STAFF";

    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final OrderManagementService orderManagementService;
    private final RestaurantTableRepository restaurantTableRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final CustomerLoyaltyService customerLoyaltyService;

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse previewCheckout(Integer orderId,
                                                   BigDecimal discount,
                                                   BigDecimal taxRate,
                                                   boolean applyLoyaltyDiscount,
                                                   CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF);

        Order order = orderManagementService.findOrderEntityById(orderId);
        enforceBillingScope(order, currentUser);
        ensureOrderReadyForCheckout(order);

        // Preview chỉ tính toán số liệu tạm thời, chưa ghi bill và chưa cộng điểm.
        PricingSnapshot pricing = buildPricingSnapshot(
                order,
                nonNegative(discount),
                nonNegative(taxRate),
                applyLoyaltyDiscount
        );

        return toPreviewResponse(order, pricing, applyLoyaltyDiscount);
    }

    @Transactional
    public BillResponse createBill(CreateBillRequest request, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF);

        Order order = orderManagementService.findOrderEntityById(request.orderId());
        enforceBillingScope(order, currentUser);
        ensureOrderReadyForCheckout(order);

        Bill existingBill = findLatestBillByOrderId(order.getId());
        if (existingBill != null && existingBill.getStatus() == BillStatus.PAID) {
            // Nếu bill cuối cùng đã PAID thì trả luôn bill đó, tránh tạo bill trùng.
            return toResponse(existingBill);
        }
        if (existingBill != null && !existingBill.getPayments().isEmpty()) {
            throw new ConflictException("Bill with recorded payments cannot be recalculated");
        }

        // Tại thời điểm tạo bill, toàn bộ discount/tax/loyalty phải được chốt thành số tiền cụ thể
        // để những lần thanh toán sau không bị tính lại lệch nhau.
        PricingSnapshot pricing = buildPricingSnapshot(
                order,
                nonNegative(request.discount()),
                nonNegative(request.taxRate()),
                Boolean.TRUE.equals(request.applyLoyaltyDiscount())
        );

        if (pricing.total().compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException("Bill total cannot be negative");
        }

        Bill bill = existingBill != null ? existingBill : Bill.builder()
                .order(order)
                .status(BillStatus.UNPAID)
                .build();

        // Giá vốn hàng bán được lấy từ stock movement đã trừ trong luồng bếp.
        // Nhờ vậy báo cáo lợi nhuận bám sát dữ liệu vận hành thực tế hơn là dùng ước lượng.
        BigDecimal costOfGoodsSold = nonNegative(stockMovementRepository.sumTotalCostByOrderId(order.getId()));
        BigDecimal grossProfit = pricing.subtotal()
                .subtract(pricing.totalDiscount())
                .subtract(costOfGoodsSold);

        bill.setSubtotal(pricing.subtotal());
        bill.setTax(pricing.tax());
        bill.setDiscount(pricing.totalDiscount());
        bill.setLoyaltyDiscount(pricing.loyaltyDiscount());
        bill.setAppliedCustomerTier(pricing.appliedTier());
        bill.setTotal(pricing.total());
        bill.setCostOfGoodsSold(costOfGoodsSold);
        bill.setGrossProfit(grossProfit);

        Bill savedBill = billRepository.save(bill);
        updateBillStatus(savedBill);
        savedBill = billRepository.save(savedBill);

        if (request.paidAmount() != null && request.paidAmount().compareTo(BigDecimal.ZERO) > 0) {
            registerPayment(savedBill, request.paidAmount(), resolvePaymentMethod(request.paymentMethod()));
        }

        finalizeOrderIfPaid(savedBill, order);
        return toResponse(reloadBill(savedBill.getId()));
    }

    @Transactional
    public BillResponse addPayment(Integer billId, CreatePaymentRequest request, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF);

        Bill bill = reloadBill(billId);
        enforceBillingScope(bill.getOrder(), currentUser);
        if (bill.getStatus() == BillStatus.PAID) {
            throw new ConflictException("Bill is already fully paid");
        }

        // Bill có thể được thanh toán nhiều lần cho đến khi đủ tổng tiền.
        registerPayment(bill, request.amount(), resolvePaymentMethod(request.method()));
        finalizeOrderIfPaid(bill, bill.getOrder());
        return toResponse(reloadBill(bill.getId()));
    }

    @Transactional(readOnly = true)
    public BillResponse getBillById(Integer billId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF);

        Bill bill = reloadBill(billId);
        enforceBillingScope(bill.getOrder(), currentUser);
        return toResponse(bill);
    }

    @Transactional(readOnly = true)
    public List<BillResponse> getBillsByOrderId(Integer orderId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF);

        // Tái sử dụng luồng đọc order hiện có để đảm bảo:
        // - order phải tồn tại
        // - user hiện tại có quyền xem order đó
        orderManagementService.getOrderById(orderId, currentUser);

        return billRepository.findAllDetailsByOrderId(orderId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<BillHistoryItemResponse> getBillHistory(Integer branchId,
                                                                BillStatus status,
                                                                LocalDate fromDate,
                                                                LocalDate toDate,
                                                                String keyword,
                                                                int page,
                                                                int size,
                                                                CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF);
        validateHistoryDateRange(fromDate, toDate);

        Integer scopedBranchId = resolveReadableHistoryBranchId(branchId, currentUser);
        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.plusDays(1).atStartOfDay() : null;

        Page<Integer> billIdPage = billRepository.searchHistoryIds(
                normalizeKeyword(keyword),
                scopedBranchId,
                status,
                fromDateTime,
                toDateTime,
                PageRequest.of(page, size)
        );

        List<BillHistoryItemResponse> content = List.of();
        if (!billIdPage.isEmpty()) {
            Map<Integer, Bill> billMap = billRepository.findAllDetailsByIds(billIdPage.getContent()).stream()
                    .collect(Collectors.toMap(Bill::getId, Function.identity(), (left, right) -> left));

            content = billIdPage.getContent().stream()
                    .map(billMap::get)
                    .filter(Objects::nonNull)
                    .map(this::toHistoryItemResponse)
                    .toList();
        }

        return PageResponse.<BillHistoryItemResponse>builder()
                .content(content)
                .page(billIdPage.getNumber())
                .size(billIdPage.getSize())
                .totalElements(billIdPage.getTotalElements())
                .totalPages(billIdPage.getTotalPages())
                .last(billIdPage.isLast())
                .build();
    }

    private PricingSnapshot buildPricingSnapshot(Order order,
                                                 BigDecimal manualDiscount,
                                                 BigDecimal taxRate,
                                                 boolean applyLoyaltyDiscount) {
        // Snapshot này gom toàn bộ con số trung gian vào một nơi:
        // subtotal -> discount -> tax -> total -> loyalty tier hiện tại / dự kiến.
        // Việc gom như vậy giúp preview và createBill dùng đúng cùng một công thức.
        BigDecimal subtotal = calculateSubtotal(order);
        BigDecimal discountableBase = subtotal.subtract(manualDiscount);
        if (discountableBase.compareTo(BigDecimal.ZERO) < 0) {
            discountableBase = BigDecimal.ZERO;
        }

        Customer customer = order.getCustomer();
        CustomerTier currentTier = customer != null ? customerLoyaltyService.resolveTier(customer.getLoyaltyPoints()) : null;
        BigDecimal loyaltyDiscount = applyLoyaltyDiscount
                ? customerLoyaltyService.calculateLoyaltyDiscount(customer, discountableBase)
                : BigDecimal.ZERO;

        // Tổng giảm giá cuối cùng = giảm thủ công + giảm theo loyalty.
        BigDecimal totalDiscount = manualDiscount.add(loyaltyDiscount);
        BigDecimal tax = subtotal.multiply(taxRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).subtract(totalDiscount);

        int currentPoints = customer != null && customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        int earnedPoints = customer != null ? customerLoyaltyService.calculateEarnedPoints(total) : 0;
        int projectedPoints = customer != null ? currentPoints + earnedPoints : 0;
        CustomerTier projectedTier = customer != null ? customerLoyaltyService.resolveTier(projectedPoints) : null;

        return new PricingSnapshot(
                subtotal,
                taxRate,
                tax,
                manualDiscount,
                loyaltyDiscount,
                totalDiscount,
                total,
                currentTier,
                applyLoyaltyDiscount ? currentTier : null,
                projectedTier,
                currentPoints,
                earnedPoints,
                projectedPoints
        );
    }

    private void registerPayment(Bill bill, BigDecimal amount, PaymentMethod method) {
        BigDecimal remaining = remainingAmount(bill);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException("Payment amount must be greater than zero");
        }
        if (amount.compareTo(remaining) > 0) {
            throw new ConflictException("Payment amount cannot exceed the remaining bill amount");
        }

        Payment payment = Payment.builder()
                .bill(bill)
                .amount(amount)
                .method(method)
                .paidAt(LocalDateTime.now())
                .build();

        bill.getPayments().add(payment);
        updateBillStatus(bill);
        paymentRepository.save(payment);
        billRepository.save(bill);
    }

    private void finalizeOrderIfPaid(Bill bill, Order order) {
        if (bill.getStatus() != BillStatus.PAID) {
            return;
        }

        // Khi bill đã thanh toán đủ:
        // 1. order được xem là hoàn tất
        // 2. bàn hoặc cả nhóm bàn chuyển sang CLEANING
        // 3. loyalty được cộng đúng một lần theo bill này
        order.setStatus(OrderStatus.COMPLETED);
        order.setClosedAt(LocalDateTime.now());

        RestaurantTable table = order.getTable();
        if (table != null) {
            List<RestaurantTable> mergedGroup = restaurantTableRepository.findAllInMergedGroups(List.of(table.getId()));
            if (mergedGroup.isEmpty()) {
                table.setStatus(RestaurantTableStatus.CLEANING);
                restaurantTableRepository.save(table);
            } else {
                // Nếu order đang nằm trên bàn gốc của nhóm gộp,
                // cả nhóm phải cùng sang CLEANING để chờ dọn dẹp.
                for (RestaurantTable groupTable : mergedGroup) {
                    groupTable.setStatus(RestaurantTableStatus.CLEANING);
                }
                restaurantTableRepository.saveAll(mergedGroup);
            }
        }

        customerLoyaltyService.rewardPaidBill(bill);
    }

    private void ensureOrderReadyForCheckout(Order order) {
        List<OrderItem> billableItems = order.getOrderItems().stream()
                .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
                .toList();

        if (billableItems.isEmpty()) {
            throw new ConflictException("Order has no billable items");
        }

        // Hệ thống chỉ cho checkout khi toàn bộ món đã đến trạng thái cuối ở phía vận hành.
        boolean hasNotServedItems = billableItems.stream().anyMatch(item ->
                item.getStatus() != OrderItemStatus.SERVED && item.getStatus() != OrderItemStatus.COMPLETED);
        if (hasNotServedItems) {
            throw new ConflictException("Order can only be checked out after all dishes are served or completed");
        }
    }

    private BigDecimal calculateSubtotal(Order order) {
        return order.getOrderItems().stream()
                .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal paidAmount(Bill bill) {
        return bill.getPayments().stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal remainingAmount(Bill bill) {
        BigDecimal remaining = nonNegative(bill.getTotal()).subtract(paidAmount(bill));
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    private void updateBillStatus(Bill bill) {
        // Quy ước:
        // - chưa có payment     -> UNPAID
        // - đã trả đủ hoặc dư   -> PAID
        // - trả một phần        -> PARTIAL
        BigDecimal paidAmount = paidAmount(bill);
        BigDecimal total = nonNegative(bill.getTotal());

        if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            bill.setStatus(BillStatus.UNPAID);
            return;
        }
        if (paidAmount.compareTo(total) >= 0) {
            bill.setStatus(BillStatus.PAID);
            return;
        }
        bill.setStatus(BillStatus.PARTIAL);
    }

    private Bill findLatestBillByOrderId(Integer orderId) {
        return billRepository.findAllDetailsByOrderId(orderId)
                .stream()
                .max(Comparator.comparing(Bill::getId))
                .orElse(null);
    }

    private Bill reloadBill(Integer billId) {
        return billRepository.findDetailById(billId)
                .orElseThrow(() -> new NotFoundException("Bill not found"));
    }

    private void validateHistoryDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }
    }

    private PaymentMethod resolvePaymentMethod(String rawMethod) {
        if (!StringUtils.hasText(rawMethod)) {
            return PaymentMethod.CASH;
        }

        try {
            return PaymentMethod.valueOf(rawMethod.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Unsupported payment method");
        }
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private CheckoutPreviewResponse toPreviewResponse(Order order, PricingSnapshot pricing, boolean applyLoyaltyDiscount) {
        Customer customer = order.getCustomer();

        return new CheckoutPreviewResponse(
                order.getId(),
                customer != null ? customer.getId() : null,
                customer != null ? customer.getName() : null,
                pricing.currentTier() != null ? pricing.currentTier().getId() : null,
                pricing.currentTier() != null ? pricing.currentTier().getCode() : null,
                pricing.currentTier() != null ? pricing.currentTier().getName() : null,
                pricing.currentTier() != null ? pricing.currentTier().getDiscountRate() : BigDecimal.ZERO,
                pricing.projectedTier() != null ? pricing.projectedTier().getId() : null,
                pricing.projectedTier() != null ? pricing.projectedTier().getCode() : null,
                pricing.projectedTier() != null ? pricing.projectedTier().getName() : null,
                pricing.projectedTier() != null ? pricing.projectedTier().getDiscountRate() : BigDecimal.ZERO,
                customer != null ? pricing.currentPoints() : null,
                customer != null ? pricing.earnedPoints() : 0,
                customer != null ? pricing.projectedPoints() : null,
                pricing.subtotal(),
                pricing.taxRate(),
                pricing.tax(),
                pricing.manualDiscount(),
                pricing.loyaltyDiscount(),
                pricing.totalDiscount(),
                pricing.total(),
                applyLoyaltyDiscount
        );
    }

    private BillResponse toResponse(Bill bill) {
        // Khi trả response, nếu giao dịch cộng điểm đã tồn tại thì lấy số thật từ transaction.
        // Nếu chưa tồn tại, response vẫn dự đoán số điểm "sẽ nhận" để frontend hiển thị trước.
        Customer customer = bill.getOrder().getCustomer();
        CustomerLoyaltyTransaction rewardTransaction = customerLoyaltyService.findEarnTransactionByBillId(bill.getId());
        int currentPoints = customer != null && customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        int projectedEarnedPoints = customer != null ? customerLoyaltyService.calculateEarnedPoints(bill.getTotal()) : 0;
        int projectedPointsAfter = customer != null ? currentPoints + projectedEarnedPoints : 0;

        BigDecimal loyaltyDiscount = nonNegative(bill.getLoyaltyDiscount());
        BigDecimal totalDiscount = nonNegative(bill.getDiscount());
        BigDecimal manualDiscount = totalDiscount.subtract(loyaltyDiscount);
        if (manualDiscount.compareTo(BigDecimal.ZERO) < 0) {
            manualDiscount = BigDecimal.ZERO;
        }

        return new BillResponse(
                bill.getId(),
                bill.getOrder().getId(),
                bill.getOrder().getTable() != null ? bill.getOrder().getTable().getId() : null,
                bill.getOrder().getTable() != null ? bill.getOrder().getTable().getTableNumber() : null,
                customer != null ? customer.getId() : null,
                customer != null ? customer.getName() : null,
                bill.getStatus().name(),
                nonNegative(bill.getSubtotal()),
                nonNegative(bill.getTax()),
                totalDiscount,
                manualDiscount,
                loyaltyDiscount,
                nonNegative(bill.getTotal()),
                paidAmount(bill),
                remainingAmount(bill),
                nonNegative(bill.getCostOfGoodsSold()),
                nonNegative(bill.getGrossProfit()),
                bill.getAppliedCustomerTier() != null ? bill.getAppliedCustomerTier().getId() : null,
                bill.getAppliedCustomerTier() != null ? bill.getAppliedCustomerTier().getCode() : null,
                bill.getAppliedCustomerTier() != null ? bill.getAppliedCustomerTier().getName() : null,
                bill.getAppliedCustomerTier() != null ? bill.getAppliedCustomerTier().getDiscountRate() : BigDecimal.ZERO,
                rewardTransaction != null,
                rewardTransaction != null ? rewardTransaction.getPointsDelta() : projectedEarnedPoints,
                rewardTransaction != null ? rewardTransaction.getPointsBefore() : (customer != null ? currentPoints : null),
                rewardTransaction != null ? rewardTransaction.getPointsAfter() : (customer != null ? projectedPointsAfter : null),
                bill.getPayments().stream()
                        .sorted(Comparator.comparing(Payment::getPaidAt))
                        .map(payment -> new PaymentResponse(
                                payment.getId(),
                                payment.getAmount(),
                                payment.getMethod().name(),
                                payment.getPaidAt()
                        ))
                        .toList()
        );
    }

    private BillHistoryItemResponse toHistoryItemResponse(Bill bill) {
        Customer customer = bill.getOrder().getCustomer();
        List<Payment> sortedPayments = bill.getPayments().stream()
                .sorted(Comparator.comparing(Payment::getPaidAt))
                .toList();

        LocalDateTime lastPaidAt = sortedPayments.stream()
                .map(Payment::getPaidAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new BillHistoryItemResponse(
                bill.getId(),
                bill.getOrder().getId(),
                bill.getOrder().getBranch().getId(),
                bill.getOrder().getBranch().getName(),
                bill.getOrder().getTable() != null ? bill.getOrder().getTable().getId() : null,
                bill.getOrder().getTable() != null ? bill.getOrder().getTable().getTableNumber() : null,
                customer != null ? customer.getId() : null,
                customer != null ? customer.getName() : null,
                bill.getStatus().name(),
                nonNegative(bill.getTotal()),
                paidAmount(bill),
                remainingAmount(bill),
                nonNegative(bill.getGrossProfit()),
                lastPaidAt,
                (long) sortedPayments.size(),
                sortedPayments.stream()
                        .map(payment -> payment.getMethod().name())
                        .distinct()
                        .toList()
        );
    }

    private void enforceBillingScope(Order order, CustomUserDetails currentUser) {
        if (hasRole(currentUser, ROLE_ADMIN) || hasRole(currentUser, ROLE_MANAGER)) {
            return;
        }

        User currentUserEntity = userRepository.findEmployeeDetailById(currentUser.getUserId())
                .orElseThrow(() -> new NotFoundException("Current user not found"));
        if (currentUserEntity.getProfile() == null || currentUserEntity.getProfile().getBranch() == null) {
            throw new ForbiddenException("Your account is not assigned to any branch");
        }

        Integer ownBranchId = currentUserEntity.getProfile().getBranch().getId();
        if (!ownBranchId.equals(order.getBranch().getId())) {
            throw new ForbiddenException("You do not have permission to settle another branch order");
        }
    }

    private Integer resolveReadableHistoryBranchId(Integer branchId, CustomUserDetails currentUser) {
        if (hasRole(currentUser, ROLE_ADMIN) || hasRole(currentUser, ROLE_MANAGER)) {
            return branchId;
        }

        Integer ownBranchId = getAssignedBranchId(currentUser);
        if (branchId != null && !branchId.equals(ownBranchId)) {
            throw new ForbiddenException("You do not have permission to view another branch bill history");
        }
        return ownBranchId;
    }

    private Integer getAssignedBranchId(CustomUserDetails currentUser) {
        User currentUserEntity = userRepository.findEmployeeDetailById(currentUser.getUserId())
                .orElseThrow(() -> new NotFoundException("Current user not found"));
        if (currentUserEntity.getProfile() == null || currentUserEntity.getProfile().getBranch() == null) {
            throw new ForbiddenException("Your account is not assigned to any branch");
        }
        return currentUserEntity.getProfile().getBranch().getId();
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
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

    private record PricingSnapshot(
            BigDecimal subtotal,
            BigDecimal taxRate,
            BigDecimal tax,
            BigDecimal manualDiscount,
            BigDecimal loyaltyDiscount,
            BigDecimal totalDiscount,
            BigDecimal total,
            CustomerTier currentTier,
            CustomerTier appliedTier,
            CustomerTier projectedTier,
            Integer currentPoints,
            Integer earnedPoints,
            Integer projectedPoints
    ) {
    }
}
