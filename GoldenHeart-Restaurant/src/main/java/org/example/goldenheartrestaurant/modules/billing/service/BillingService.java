package org.example.goldenheartrestaurant.modules.billing.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.billing.dto.request.CreateBillRequest;
import org.example.goldenheartrestaurant.modules.billing.dto.request.CreatePaymentRequest;
import org.example.goldenheartrestaurant.modules.billing.dto.response.BillResponse;
import org.example.goldenheartrestaurant.modules.billing.dto.response.PaymentResponse;
import org.example.goldenheartrestaurant.modules.billing.entity.Bill;
import org.example.goldenheartrestaurant.modules.billing.entity.BillStatus;
import org.example.goldenheartrestaurant.modules.billing.entity.Payment;
import org.example.goldenheartrestaurant.modules.billing.entity.PaymentMethod;
import org.example.goldenheartrestaurant.modules.billing.repository.BillRepository;
import org.example.goldenheartrestaurant.modules.billing.repository.PaymentRepository;
import org.example.goldenheartrestaurant.modules.customer.entity.Customer;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

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

    @Transactional
    public BillResponse createBill(CreateBillRequest request, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF);

        Order order = orderManagementService.findOrderEntityById(request.orderId());
        enforceBillingScope(order, currentUser);
        ensureOrderReadyForCheckout(order);

        Bill existingBill = findLatestBillByOrderId(order.getId());
        if (existingBill != null && existingBill.getStatus() == BillStatus.PAID) {
            return toResponse(existingBill);
        }

        Bill bill = existingBill != null ? existingBill : Bill.builder()
                .order(order)
                .status(BillStatus.UNPAID)
                .build();

        BigDecimal subtotal = calculateSubtotal(order);
        BigDecimal taxRate = nonNegative(request.taxRate());
        BigDecimal discount = nonNegative(request.discount());
        BigDecimal tax = subtotal.multiply(taxRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).subtract(discount);

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException("Bill total cannot be negative");
        }

        BigDecimal costOfGoodsSold = stockMovementRepository.sumTotalCostByOrderId(order.getId());
        BigDecimal grossProfit = subtotal.subtract(discount).subtract(nonNegative(costOfGoodsSold));

        bill.setSubtotal(subtotal);
        bill.setTax(tax);
        bill.setDiscount(discount);
        bill.setTotal(total);
        bill.setCostOfGoodsSold(nonNegative(costOfGoodsSold));
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

        registerPayment(bill, request.amount(), resolvePaymentMethod(request.method()));
        finalizeOrderIfPaid(bill, bill.getOrder());
        return toResponse(reloadBill(bill.getId()));
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

        order.setStatus(OrderStatus.COMPLETED);
        order.setClosedAt(LocalDateTime.now());

        RestaurantTable table = order.getTable();
        if (table != null) {
            table.setStatus(RestaurantTableStatus.CLEANING);
            restaurantTableRepository.save(table);
        }

        Customer customer = order.getCustomer();
        if (customer != null) {
            customer.setLastVisitAt(LocalDateTime.now());
        }
    }

    private void ensureOrderReadyForCheckout(Order order) {
        List<OrderItem> billableItems = order.getOrderItems().stream()
                .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
                .toList();

        if (billableItems.isEmpty()) {
            throw new ConflictException("Order has no billable items");
        }

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

    private BillResponse toResponse(Bill bill) {
        return new BillResponse(
                bill.getId(),
                bill.getOrder().getId(),
                bill.getOrder().getTable() != null ? bill.getOrder().getTable().getId() : null,
                bill.getOrder().getTable() != null ? bill.getOrder().getTable().getTableNumber() : null,
                bill.getStatus().name(),
                nonNegative(bill.getSubtotal()),
                nonNegative(bill.getTax()),
                nonNegative(bill.getDiscount()),
                nonNegative(bill.getTotal()),
                paidAmount(bill),
                remainingAmount(bill),
                nonNegative(bill.getCostOfGoodsSold()),
                nonNegative(bill.getGrossProfit()),
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
}
