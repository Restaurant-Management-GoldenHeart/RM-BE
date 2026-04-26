package org.example.goldenheartrestaurant.modules.report.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.billing.entity.BillStatus;
import org.example.goldenheartrestaurant.modules.billing.entity.PaymentMethod;
import org.example.goldenheartrestaurant.modules.billing.repository.BillRepository;
import org.example.goldenheartrestaurant.modules.billing.repository.PaymentRepository;
import org.example.goldenheartrestaurant.modules.customer.repository.CustomerRepository;
import org.example.goldenheartrestaurant.modules.identity.entity.UserProfile;
import org.example.goldenheartrestaurant.modules.identity.entity.UserStatus;
import org.example.goldenheartrestaurant.modules.identity.repository.UserProfileRepository;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.InventoryRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.projection.InventorySummaryProjection;
import org.example.goldenheartrestaurant.modules.menu.repository.MenuItemRepository;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItemStatus;
import org.example.goldenheartrestaurant.modules.order.repository.OrderItemRepository;
import org.example.goldenheartrestaurant.modules.order.repository.OrderRepository;
import org.example.goldenheartrestaurant.modules.report.dto.request.ReportGroupBy;
import org.example.goldenheartrestaurant.modules.report.dto.request.ReportPeriodType;
import org.example.goldenheartrestaurant.modules.report.dto.response.BillStatusSummaryResponse;
import org.example.goldenheartrestaurant.modules.report.dto.response.DashboardReportResponse;
import org.example.goldenheartrestaurant.modules.report.dto.response.PaymentMethodBreakdownItemResponse;
import org.example.goldenheartrestaurant.modules.report.dto.response.PaymentMethodBreakdownResponse;
import org.example.goldenheartrestaurant.modules.report.dto.response.RevenueSummaryResponse;
import org.example.goldenheartrestaurant.modules.report.dto.response.RevenueTimeseriesPointResponse;
import org.example.goldenheartrestaurant.modules.report.dto.response.RevenueTimeseriesResponse;
import org.example.goldenheartrestaurant.modules.report.repository.projection.BillStatusCountProjection;
import org.example.goldenheartrestaurant.modules.report.repository.projection.OrderItemStatusCountProjection;
import org.example.goldenheartrestaurant.modules.report.repository.projection.PaidBillRevenueSummaryProjection;
import org.example.goldenheartrestaurant.modules.report.repository.projection.PaymentMethodBreakdownProjection;
import org.example.goldenheartrestaurant.modules.report.repository.projection.PaymentRevenueSummaryProjection;
import org.example.goldenheartrestaurant.modules.report.repository.projection.TableStatusCountProjection;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.example.goldenheartrestaurant.modules.restaurant.entity.RestaurantTableStatus;
import org.example.goldenheartrestaurant.modules.restaurant.repository.BranchRepository;
import org.example.goldenheartrestaurant.modules.restaurant.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * Service tong hop du lieu dashboard va bao cao.
 *
 * Nguyen tac thiet ke:
 * - Tan dung query aggregation tai DB, khong keo list len roi cong tay.
 * - Scope branch giong cac module nghiep vu khac:
 *   + ADMIN xem duoc tat ca hoac theo branch cu the.
 *   + MANAGER/STAFF/KITCHEN chi xem branch cua minh.
 * - Bao cao tai chinh chi mo cho ADMIN va MANAGER.
 * - "cash in" duoc tinh theo payment.paidAt.
 * - "paid bill revenue" duoc tinh theo bill da PAID, va thoi diem hoan tat bill
 *   duoc suy ra tu payment cuoi cung cua bill.
 */
public class ReportService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_STAFF = "STAFF";
    private static final String ROLE_KITCHEN = "KITCHEN";
    private static final String ALL_BRANCHES_LABEL = "All branches";

    private static final List<String> EMPLOYEE_ROLES = List.of(ROLE_MANAGER, ROLE_STAFF, ROLE_KITCHEN);
    private static final List<OrderItemStatus> KITCHEN_REPORT_STATUSES = List.of(
            OrderItemStatus.PENDING,
            OrderItemStatus.PROCESSING,
            OrderItemStatus.WAITING_STOCK
    );

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CustomerRepository customerRepository;
    private final MenuItemRepository menuItemRepository;
    private final InventoryRepository inventoryRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public DashboardReportResponse getDashboardReport(Integer branchId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF, ROLE_KITCHEN);

        Integer scopedBranchId = resolveReadableBranchId(branchId, currentUser);
        String branchName = resolveBranchName(scopedBranchId);
        InventorySummaryProjection inventorySummary = inventoryRepository.summarizeCurrentStateByBranch(scopedBranchId);
        long totalInventoryItems = inventorySummary != null ? defaultIfNull(inventorySummary.getTotalItems()) : 0L;
        BigDecimal totalInventoryValue = inventorySummary != null ? defaultIfNull(inventorySummary.getTotalInventoryValue()) : BigDecimal.ZERO;
        long lowStockItems = inventorySummary != null ? defaultIfNull(inventorySummary.getLowStockCount()) : 0L;
        long outOfStockItems = inventorySummary != null ? defaultIfNull(inventorySummary.getOutOfStockCount()) : 0L;
        Map<RestaurantTableStatus, Long> tableCounts = mapTableCounts(
                restaurantTableRepository.countByStatusForReport(scopedBranchId)
        );
        Map<OrderItemStatus, Long> kitchenCounts = mapKitchenCounts(
                orderItemRepository.countKitchenItemsByStatusesForReport(KITCHEN_REPORT_STATUSES, scopedBranchId)
        );
        RevenueMetrics todayMetrics = summarizeRevenueMetrics(scopedBranchId, new DateRange(LocalDate.now(), LocalDate.now()));

        return new DashboardReportResponse(
                scopedBranchId,
                branchName,
                userRepository.countActiveEmployeesByBranchAndRoles(scopedBranchId, EMPLOYEE_ROLES, UserStatus.ACTIVE),
                customerRepository.countCustomersForReport(scopedBranchId),
                menuItemRepository.countForReport(scopedBranchId),
                totalInventoryItems,
                totalInventoryValue,
                lowStockItems,
                outOfStockItems,
                defaultStatusCount(tableCounts, RestaurantTableStatus.AVAILABLE),
                defaultStatusCount(tableCounts, RestaurantTableStatus.OCCUPIED),
                defaultStatusCount(tableCounts, RestaurantTableStatus.RESERVED),
                defaultStatusCount(tableCounts, RestaurantTableStatus.CLEANING),
                orderRepository.countActiveOrdersForReport(scopedBranchId),
                defaultStatusCount(kitchenCounts, OrderItemStatus.PENDING),
                defaultStatusCount(kitchenCounts, OrderItemStatus.PROCESSING),
                defaultStatusCount(kitchenCounts, OrderItemStatus.WAITING_STOCK),
                todayMetrics.paymentCount(),
                todayMetrics.paidBillsCount(),
                todayMetrics.cashIn(),
                todayMetrics.paidBillRevenue(),
                todayMetrics.grossProfit(),
                todayMetrics.averagePaidBillValue(),
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public RevenueSummaryResponse getRevenueSummary(Integer branchId,
                                                   ReportPeriodType periodType,
                                                   LocalDate anchorDate,
                                                   CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER);

        Integer scopedBranchId = resolveReadableBranchId(branchId, currentUser);
        ReportPeriodType effectivePeriodType = periodType != null ? periodType : ReportPeriodType.MONTH;
        LocalDate effectiveAnchorDate = anchorDate != null ? anchorDate : LocalDate.now();
        DateRange range = resolveSummaryRange(effectivePeriodType, effectiveAnchorDate);
        RevenueMetrics metrics = summarizeRevenueMetrics(scopedBranchId, range);

        return new RevenueSummaryResponse(
                scopedBranchId,
                resolveBranchName(scopedBranchId),
                effectivePeriodType.name(),
                range.fromDate(),
                range.toDate(),
                metrics.paymentCount(),
                metrics.cashIn(),
                metrics.paidBillsCount(),
                metrics.paidBillRevenue(),
                metrics.grossProfit(),
                metrics.averagePaidBillValue(),
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public RevenueTimeseriesResponse getRevenueTimeseries(Integer branchId,
                                                          LocalDate fromDate,
                                                          LocalDate toDate,
                                                          ReportGroupBy groupBy,
                                                          CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER);

        Integer scopedBranchId = resolveReadableBranchId(branchId, currentUser);
        ReportGroupBy effectiveGroupBy = groupBy != null ? groupBy : ReportGroupBy.DAY;
        DateRange normalizedRange = normalizeTimeseriesRange(fromDate, toDate);

        List<BucketRange> buckets = buildBuckets(normalizedRange, effectiveGroupBy);
        List<RevenueTimeseriesPointResponse> points = new ArrayList<>();

        long totalPaymentCount = 0L;
        long totalPaidBillsCount = 0L;
        BigDecimal totalCashIn = BigDecimal.ZERO;
        BigDecimal totalPaidBillRevenue = BigDecimal.ZERO;
        BigDecimal totalGrossProfit = BigDecimal.ZERO;

        for (BucketRange bucket : buckets) {
            RevenueMetrics metrics = summarizeRevenueMetrics(scopedBranchId, new DateRange(bucket.fromDate(), bucket.toDate()));
            points.add(new RevenueTimeseriesPointResponse(
                    bucket.periodKey(),
                    bucket.fromDate(),
                    bucket.toDate(),
                    metrics.paymentCount(),
                    metrics.cashIn(),
                    metrics.paidBillsCount(),
                    metrics.paidBillRevenue(),
                    metrics.grossProfit(),
                    metrics.averagePaidBillValue()
            ));

            totalPaymentCount += metrics.paymentCount();
            totalPaidBillsCount += metrics.paidBillsCount();
            totalCashIn = totalCashIn.add(metrics.cashIn());
            totalPaidBillRevenue = totalPaidBillRevenue.add(metrics.paidBillRevenue());
            totalGrossProfit = totalGrossProfit.add(metrics.grossProfit());
        }

        return new RevenueTimeseriesResponse(
                scopedBranchId,
                resolveBranchName(scopedBranchId),
                effectiveGroupBy.name(),
                normalizedRange.fromDate(),
                normalizedRange.toDate(),
                totalPaymentCount,
                totalCashIn,
                totalPaidBillsCount,
                totalPaidBillRevenue,
                totalGrossProfit,
                points,
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public PaymentMethodBreakdownResponse getPaymentMethodBreakdown(Integer branchId,
                                                                   ReportPeriodType periodType,
                                                                   LocalDate anchorDate,
                                                                   CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER);

        Integer scopedBranchId = resolveReadableBranchId(branchId, currentUser);
        ReportPeriodType effectivePeriodType = periodType != null ? periodType : ReportPeriodType.MONTH;
        LocalDate effectiveAnchorDate = anchorDate != null ? anchorDate : LocalDate.now();
        DateRange range = resolveSummaryRange(effectivePeriodType, effectiveAnchorDate);

        List<PaymentMethodBreakdownProjection> projections = paymentRepository.summarizeByMethodForReport(
                scopedBranchId,
                range.fromDate().atStartOfDay(),
                range.toDate().plusDays(1).atStartOfDay()
        );
        Map<PaymentMethod, PaymentMethodBreakdownProjection> projectionMap = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethodBreakdownProjection projection : projections) {
            projectionMap.put(projection.getMethod(), projection);
        }

        long totalPaymentCount = 0L;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PaymentMethodBreakdownProjection projection : projections) {
            totalPaymentCount += defaultIfNull(projection.getPaymentCount());
            totalAmount = totalAmount.add(defaultIfNull(projection.getTotalAmount()));
        }

        List<PaymentMethodBreakdownItemResponse> items = new ArrayList<>();
        for (PaymentMethod method : PaymentMethod.values()) {
            PaymentMethodBreakdownProjection projection = projectionMap.get(method);
            long paymentCount = projection != null ? defaultIfNull(projection.getPaymentCount()) : 0L;
            BigDecimal amount = projection != null ? defaultIfNull(projection.getTotalAmount()) : BigDecimal.ZERO;
            items.add(new PaymentMethodBreakdownItemResponse(
                    method.name(),
                    paymentCount,
                    amount,
                    calculatePercentage(amount, totalAmount)
            ));
        }

        return new PaymentMethodBreakdownResponse(
                scopedBranchId,
                resolveBranchName(scopedBranchId),
                effectivePeriodType.name(),
                range.fromDate(),
                range.toDate(),
                totalPaymentCount,
                totalAmount,
                items,
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public BillStatusSummaryResponse getBillStatusSummary(Integer branchId, CustomUserDetails currentUser) {
        requireAnyRole(currentUser, ROLE_ADMIN, ROLE_MANAGER);

        Integer scopedBranchId = resolveReadableBranchId(branchId, currentUser);
        Map<BillStatus, Long> billStatusCounts = mapBillStatusCounts(billRepository.countByStatusForReport(scopedBranchId));

        long unpaidBills = defaultStatusCount(billStatusCounts, BillStatus.UNPAID);
        long partialBills = defaultStatusCount(billStatusCounts, BillStatus.PARTIAL);
        long paidBills = defaultStatusCount(billStatusCounts, BillStatus.PAID);

        return new BillStatusSummaryResponse(
                scopedBranchId,
                resolveBranchName(scopedBranchId),
                unpaidBills,
                partialBills,
                paidBills,
                unpaidBills + partialBills + paidBills,
                LocalDateTime.now()
        );
    }

    private RevenueMetrics summarizeRevenueMetrics(Integer branchId, DateRange range) {
        LocalDateTime fromDateTime = range.fromDate().atStartOfDay();
        LocalDateTime toDateTime = range.toDate().plusDays(1).atStartOfDay();

        PaymentRevenueSummaryProjection paymentProjection = paymentRepository.summarizeCashInForReport(
                branchId,
                fromDateTime,
                toDateTime
        );
        PaidBillRevenueSummaryProjection paidBillProjection = billRepository.summarizePaidBillsByCompletedAtForReport(
                branchId,
                fromDateTime,
                toDateTime
        );

        long paymentCount = paymentProjection != null ? defaultIfNull(paymentProjection.getPaymentCount()) : 0L;
        BigDecimal cashIn = paymentProjection != null ? defaultIfNull(paymentProjection.getCashIn()) : BigDecimal.ZERO;
        long paidBillsCount = paidBillProjection != null ? defaultIfNull(paidBillProjection.getPaidBillsCount()) : 0L;
        BigDecimal paidBillRevenue = paidBillProjection != null
                ? defaultIfNull(paidBillProjection.getPaidBillRevenue())
                : BigDecimal.ZERO;
        BigDecimal grossProfit = paidBillProjection != null
                ? defaultIfNull(paidBillProjection.getGrossProfit())
                : BigDecimal.ZERO;

        BigDecimal averagePaidBillValue = paidBillsCount == 0
                ? BigDecimal.ZERO
                : paidBillRevenue.divide(BigDecimal.valueOf(paidBillsCount), 2, RoundingMode.HALF_UP);

        return new RevenueMetrics(
                paymentCount,
                cashIn,
                paidBillsCount,
                paidBillRevenue,
                grossProfit,
                averagePaidBillValue
        );
    }

    private DateRange resolveSummaryRange(ReportPeriodType periodType, LocalDate anchorDate) {
        return switch (periodType) {
            case DAY -> new DateRange(anchorDate, anchorDate);
            case WEEK -> {
                LocalDate weekStart = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new DateRange(weekStart, weekStart.plusDays(6));
            }
            case MONTH -> {
                LocalDate monthStart = anchorDate.withDayOfMonth(1);
                yield new DateRange(monthStart, anchorDate.withDayOfMonth(anchorDate.lengthOfMonth()));
            }
            case YEAR -> {
                LocalDate yearStart = anchorDate.withDayOfYear(1);
                yield new DateRange(yearStart, anchorDate.withDayOfYear(anchorDate.lengthOfYear()));
            }
        };
    }

    private DateRange normalizeTimeseriesRange(LocalDate fromDate, LocalDate toDate) {
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

        return new DateRange(effectiveFromDate, effectiveToDate);
    }

    private List<BucketRange> buildBuckets(DateRange range, ReportGroupBy groupBy) {
        List<BucketRange> buckets = new ArrayList<>();

        switch (groupBy) {
            case DAY -> {
                LocalDate cursor = range.fromDate();
                while (!cursor.isAfter(range.toDate())) {
                    buckets.add(new BucketRange(cursor.toString(), cursor, cursor));
                    cursor = cursor.plusDays(1);
                }
            }
            case WEEK -> {
                LocalDate cursor = range.fromDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                while (!cursor.isAfter(range.toDate())) {
                    LocalDate bucketFrom = maxDate(cursor, range.fromDate());
                    LocalDate bucketTo = minDate(cursor.plusDays(6), range.toDate());
                    buckets.add(new BucketRange(formatWeekKey(cursor), bucketFrom, bucketTo));
                    cursor = cursor.plusWeeks(1);
                }
            }
            case MONTH -> {
                LocalDate cursor = range.fromDate().withDayOfMonth(1);
                while (!cursor.isAfter(range.toDate())) {
                    LocalDate monthEnd = cursor.withDayOfMonth(cursor.lengthOfMonth());
                    LocalDate bucketFrom = maxDate(cursor, range.fromDate());
                    LocalDate bucketTo = minDate(monthEnd, range.toDate());
                    buckets.add(new BucketRange(cursor.getYear() + "-" + String.format("%02d", cursor.getMonthValue()), bucketFrom, bucketTo));
                    cursor = cursor.plusMonths(1).withDayOfMonth(1);
                }
            }
        }

        return buckets;
    }

    private Integer resolveReadableBranchId(Integer branchId, CustomUserDetails currentUser) {
        if (hasRole(currentUser, ROLE_ADMIN)) {
            return branchId;
        }

        Integer ownBranchId = getAssignedBranchId(currentUser);
        if (branchId != null && !branchId.equals(ownBranchId)) {
            throw new ForbiddenException("You do not have permission to view another branch report");
        }
        return ownBranchId;
    }

    private Integer getAssignedBranchId(CustomUserDetails currentUser) {
        UserProfile profile = userProfileRepository.findActiveDetailByUserId(currentUser.getUserId())
                .orElseThrow(() -> new NotFoundException("Current user profile not found"));

        if (profile.getBranch() == null) {
            throw new ForbiddenException("Your account is not assigned to any branch");
        }

        return profile.getBranch().getId();
    }

    private String resolveBranchName(Integer branchId) {
        if (branchId == null) {
            return ALL_BRANCHES_LABEL;
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found"));
        return branch.getName();
    }

    private Map<RestaurantTableStatus, Long> mapTableCounts(List<TableStatusCountProjection> projections) {
        Map<RestaurantTableStatus, Long> counts = new EnumMap<>(RestaurantTableStatus.class);
        for (TableStatusCountProjection projection : projections) {
            counts.put(projection.getStatus(), defaultIfNull(projection.getTotal()));
        }
        return counts;
    }

    private Map<OrderItemStatus, Long> mapKitchenCounts(List<OrderItemStatusCountProjection> projections) {
        Map<OrderItemStatus, Long> counts = new EnumMap<>(OrderItemStatus.class);
        for (OrderItemStatusCountProjection projection : projections) {
            counts.put(projection.getStatus(), defaultIfNull(projection.getTotal()));
        }
        return counts;
    }

    private Map<BillStatus, Long> mapBillStatusCounts(List<BillStatusCountProjection> projections) {
        Map<BillStatus, Long> counts = new EnumMap<>(BillStatus.class);
        for (BillStatusCountProjection projection : projections) {
            counts.put(projection.getStatus(), defaultIfNull(projection.getTotal()));
        }
        return counts;
    }

    private String formatWeekKey(LocalDate date) {
        WeekFields weekFields = WeekFields.ISO;
        int weekNumber = date.get(weekFields.weekOfWeekBasedYear());
        int weekYear = date.get(weekFields.weekBasedYear());
        return weekYear + "-W" + String.format("%02d", weekNumber);
    }

    private LocalDate maxDate(LocalDate left, LocalDate right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalDate minDate(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }

    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal totalAmount) {
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(BigDecimal.valueOf(100))
                .divide(totalAmount, 2, RoundingMode.HALF_UP);
    }

    private Long defaultIfNull(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private <T extends Enum<T>> long defaultStatusCount(Map<T, Long> counts, T status) {
        return counts.getOrDefault(status, 0L);
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

    private record DateRange(LocalDate fromDate, LocalDate toDate) {
    }

    private record BucketRange(String periodKey, LocalDate fromDate, LocalDate toDate) {
    }

    private record RevenueMetrics(
            Long paymentCount,
            BigDecimal cashIn,
            Long paidBillsCount,
            BigDecimal paidBillRevenue,
            BigDecimal grossProfit,
            BigDecimal averagePaidBillValue
    ) {
    }
}
