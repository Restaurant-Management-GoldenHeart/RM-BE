package org.example.goldenheartrestaurant.modules.customer.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.modules.billing.entity.Bill;
import org.example.goldenheartrestaurant.modules.billing.entity.BillStatus;
import org.example.goldenheartrestaurant.modules.customer.dto.response.CustomerLoyaltyTransactionResponse;
import org.example.goldenheartrestaurant.modules.customer.entity.Customer;
import org.example.goldenheartrestaurant.modules.customer.entity.CustomerLoyaltyTransaction;
import org.example.goldenheartrestaurant.modules.customer.entity.CustomerLoyaltyTransactionType;
import org.example.goldenheartrestaurant.modules.customer.entity.CustomerTier;
import org.example.goldenheartrestaurant.modules.customer.repository.CustomerLoyaltyTransactionRepository;
import org.example.goldenheartrestaurant.modules.customer.repository.CustomerRepository;
import org.example.goldenheartrestaurant.modules.customer.repository.CustomerTierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
/**
 * Service tap trung logic loyalty:
 * - xac dinh tier hien tai theo diem
 * - tinh discount loyalty
 * - cong diem khi bill da PAID
 * - tra lich su cong/tru diem
 */
public class CustomerLoyaltyService {

    private static final BigDecimal POINTS_DIVISOR = BigDecimal.valueOf(10_000);

    private final CustomerRepository customerRepository;
    private final CustomerTierRepository customerTierRepository;
    private final CustomerLoyaltyTransactionRepository customerLoyaltyTransactionRepository;

    @Transactional(readOnly = true)
    public CustomerTier resolveTier(Integer loyaltyPoints) {
        return customerTierRepository
                .findFirstByActiveTrueAndMinPointsLessThanEqualOrderByMinPointsDesc(safePoints(loyaltyPoints))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public BigDecimal resolveDiscountRate(Integer loyaltyPoints) {
        CustomerTier tier = resolveTier(loyaltyPoints);
        return tier != null ? tier.getDiscountRate() : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateLoyaltyDiscount(Customer customer, BigDecimal discountableBase) {
        if (customer == null || discountableBase == null || discountableBase.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal rate = resolveDiscountRate(customer.getLoyaltyPoints());
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return discountableBase.multiply(rate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public int calculateEarnedPoints(BigDecimal paidTotal) {
        if (paidTotal == null || paidTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        return paidTotal.divide(POINTS_DIVISOR, 0, RoundingMode.DOWN).intValue();
    }

    @Transactional
    public CustomerLoyaltyTransaction rewardPaidBill(Bill bill) {
        if (bill.getStatus() != BillStatus.PAID) {
            return null;
        }
        if (bill.getOrder() == null || bill.getOrder().getCustomer() == null) {
            return null;
        }
        if (customerLoyaltyTransactionRepository.existsByBill_IdAndType(bill.getId(), CustomerLoyaltyTransactionType.EARN)) {
            return customerLoyaltyTransactionRepository.findFirstByBill_IdAndType(bill.getId(), CustomerLoyaltyTransactionType.EARN)
                    .orElse(null);
        }

        Customer customer = customerRepository.findByIdForUpdate(bill.getOrder().getCustomer().getId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        int pointsBefore = safePoints(customer.getLoyaltyPoints());
        int pointsDelta = calculateEarnedPoints(bill.getTotal());
        int pointsAfter = pointsBefore + pointsDelta;

        customer.setLoyaltyPoints(pointsAfter);
        customer.setLastVisitAt(LocalDateTime.now());

        CustomerLoyaltyTransaction transaction = CustomerLoyaltyTransaction.builder()
                .customer(customer)
                .bill(bill)
                .type(CustomerLoyaltyTransactionType.EARN)
                .pointsDelta(pointsDelta)
                .pointsBefore(pointsBefore)
                .pointsAfter(pointsAfter)
                .description("Tich diem tu hoa don #" + bill.getId())
                .createdAt(LocalDateTime.now())
                .build();

        return customerLoyaltyTransactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public CustomerLoyaltyTransaction findEarnTransactionByBillId(Integer billId) {
        return customerLoyaltyTransactionRepository.findFirstByBill_IdAndType(billId, CustomerLoyaltyTransactionType.EARN)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerLoyaltyTransactionResponse> getCustomerTransactions(Integer customerId, int page, int size) {
        Page<CustomerLoyaltyTransaction> transactions = customerLoyaltyTransactionRepository
                .findByCustomer_IdOrderByCreatedAtDesc(customerId, PageRequest.of(page, size));

        return PageResponse.<CustomerLoyaltyTransactionResponse>builder()
                .content(transactions.getContent().stream().map(this::toTransactionResponse).toList())
                .page(transactions.getNumber())
                .size(transactions.getSize())
                .totalElements(transactions.getTotalElements())
                .totalPages(transactions.getTotalPages())
                .last(transactions.isLast())
                .build();
    }

    private CustomerLoyaltyTransactionResponse toTransactionResponse(CustomerLoyaltyTransaction transaction) {
        return new CustomerLoyaltyTransactionResponse(
                transaction.getId(),
                transaction.getCustomer().getId(),
                transaction.getBill() != null ? transaction.getBill().getId() : null,
                transaction.getType().name(),
                transaction.getPointsDelta(),
                transaction.getPointsBefore(),
                transaction.getPointsAfter(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }

    private int safePoints(Integer loyaltyPoints) {
        return loyaltyPoints == null ? 0 : loyaltyPoints;
    }
}
