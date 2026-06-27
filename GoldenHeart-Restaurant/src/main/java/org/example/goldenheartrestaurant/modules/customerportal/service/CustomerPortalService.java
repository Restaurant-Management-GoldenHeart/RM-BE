package org.example.goldenheartrestaurant.modules.customerportal.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.BadRequestException;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.ForbiddenException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.modules.billing.entity.Bill;
import org.example.goldenheartrestaurant.modules.billing.entity.BillStatus;
import org.example.goldenheartrestaurant.modules.coupon.entity.CustomerCoupon;
import org.example.goldenheartrestaurant.modules.coupon.entity.CustomerCouponStatus;
import org.example.goldenheartrestaurant.modules.coupon.repository.CustomerCouponRepository;
import org.example.goldenheartrestaurant.modules.customer.entity.Customer;
import org.example.goldenheartrestaurant.modules.customer.entity.CustomerTier;
import org.example.goldenheartrestaurant.modules.customer.repository.CustomerLoyaltyTransactionRepository;
import org.example.goldenheartrestaurant.modules.customer.repository.CustomerRepository;
import org.example.goldenheartrestaurant.modules.customer.repository.CustomerTierRepository;
import org.example.goldenheartrestaurant.modules.customer.dto.response.CustomerLoyaltyTransactionResponse;
import org.example.goldenheartrestaurant.modules.customer.service.CustomerLoyaltyService;
import org.example.goldenheartrestaurant.modules.customerportal.dto.request.CreateReviewRequest;
import org.example.goldenheartrestaurant.modules.customerportal.dto.request.UpdateCustomerProfileRequest;
import org.example.goldenheartrestaurant.modules.customerportal.dto.response.*;
import org.example.goldenheartrestaurant.modules.order.entity.Order;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItem;
import org.example.goldenheartrestaurant.modules.order.repository.OrderItemRepository;
import org.example.goldenheartrestaurant.modules.order.repository.OrderRepository;
import org.example.goldenheartrestaurant.modules.review.entity.Review;
import org.example.goldenheartrestaurant.modules.review.entity.ReviewStatus;
import org.example.goldenheartrestaurant.modules.review.entity.ReviewType;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.example.goldenheartrestaurant.modules.restaurant.repository.BranchRepository;
import org.example.goldenheartrestaurant.modules.review.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service trung tâm của Customer Portal.
 *
 * Mọi nghiệp vụ đều bắt đầu bằng việc resolve userId → Customer CRM
 * qua phương thức {@link #requireCustomerByUserId(Integer)}.
 * Nếu khách chưa có bản ghi CRM (trường hợp đăng ký cũ trước khi feature này ra đời),
 * sẽ ném NotFoundException để FE hiển thị hướng dẫn liên hệ nhà hàng.
 */
@Service
@RequiredArgsConstructor
public class CustomerPortalService {

    private final CustomerRepository customerRepository;
    private final CustomerTierRepository customerTierRepository;
    private final CustomerLoyaltyService customerLoyaltyService;
    private final CustomerLoyaltyTransactionRepository loyaltyTransactionRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;
    private final BranchRepository branchRepository;
    private final CustomerCouponRepository customerCouponRepository;

    // ─── Profile ────────────────────────────────────────────────────────────────

    /**
     * Lấy thông tin hồ sơ đầy đủ của khách hàng đang đăng nhập.
     * Bao gồm tier hiện tại và tier tiếp theo để FE vẽ thanh tiến trình điểm.
     */
    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(Integer userId) {
        Customer customer = requireCustomerByUserId(userId);

        int points = safePoints(customer.getLoyaltyPoints());
        CustomerTier currentTier = customerLoyaltyService.resolveTier(points);

        // Tìm tier kế tiếp (tier có min_points lớn hơn điểm hiện tại)
        CustomerTier nextTier = customerTierRepository
                .findFirstByActiveTrueAndMinPointsGreaterThanOrderByMinPointsAsc(points)
                .orElse(null);

        return new CustomerProfileResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getDateOfBirth(),
                customer.getGender(),
                points,
                customer.getLastVisitAt(),
                currentTier != null ? currentTier.getCode() : null,
                currentTier != null ? currentTier.getName() : null,
                currentTier != null ? currentTier.getDiscountRate() : null,
                nextTier != null ? nextTier.getCode() : null,
                nextTier != null ? nextTier.getName() : null,
                nextTier != null ? nextTier.getMinPoints() : null,
                customer.getCreatedAt()
        );
    }

    /**
     * Cập nhật thông tin cá nhân của khách — chỉ cho phép sửa trường an toàn.
     * Email và điểm tích luỹ không thể tự thay đổi.
     */
    @Transactional
    public CustomerProfileResponse updateProfile(Integer userId, UpdateCustomerProfileRequest request) {
        Customer customer = requireCustomerByUserId(userId);

        if (StringUtils.hasText(request.name())) {
            customer.setName(request.name().trim());
        }
        if (request.phone() != null) {
            // Kiểm tra số điện thoại chưa được dùng bởi khách hàng khác
            if (StringUtils.hasText(request.phone())
                    && customerRepository.existsByActivePhoneAndIdNot(request.phone(), customer.getId())) {
                throw new ConflictException("Số điện thoại đã được sử dụng bởi tài khoản khác");
            }
            customer.setPhone(request.phone());
            customer.setActivePhone(request.phone());
        }
        if (request.address() != null) {
            customer.setAddress(request.address());
        }
        if (request.dateOfBirth() != null) {
            customer.setDateOfBirth(request.dateOfBirth());
        }
        if (request.gender() != null) {
            customer.setGender(request.gender());
        }

        customerRepository.save(customer);
        return getProfile(userId);
    }

    // ─── Loyalty ────────────────────────────────────────────────────────────────

    /**
     * Lịch sử tích/trừ điểm tích luỹ của khách, sắp xếp mới nhất trước.
     * Mỗi bản ghi ghi rõ loại giao dịch (EARN/ADJUSTMENT), số điểm trước và sau.
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomerLoyaltyTransactionResponse> getLoyaltyTransactions(
            Integer userId, int page, int size) {
        Customer customer = requireCustomerByUserId(userId);
        return customerLoyaltyService.getCustomerTransactions(customer.getId(), page, size);
    }

    // ─── Lịch sử đơn hàng ───────────────────────────────────────────────────────

    /**
     * Lịch sử đơn hàng của khách, có phân trang.
     * Mỗi đơn gồm danh sách món, trạng thái thanh toán và điểm tích từ đơn đó.
     * Với mỗi dòng OrderItem, trả thêm cờ "reviewed" để FE biết hiện nút "Viết đánh giá" không.
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomerOrderResponse> getOrderHistory(Integer userId, int page, int size) {
        Customer customer = requireCustomerByUserId(userId);
        Integer customerId = customer.getId();

        // Lấy trang danh sách order (đã fetch kèm items và menuItem)
        Pageable pageable = PageRequest.of(page, size);
        List<Order> orders = orderRepository.findByCustomerIdForPortal(customerId, pageable);
        long total = orderRepository.countByCustomerId(customerId);

        // Lấy tập hợp orderItemId đã được đánh giá để đánh dấu cờ reviewed
        Set<Integer> reviewedOrderItemIds = orders.stream()
                .flatMap(o -> o.getOrderItems().stream())
                .map(oi -> oi.getId())
                .filter(id -> reviewRepository.existsByOrderItemId(id))
                .collect(Collectors.toSet());

        List<CustomerOrderResponse> content = orders.stream()
                .map(o -> toOrderResponse(o, reviewedOrderItemIds))
                .toList();

        return PageResponse.<CustomerOrderResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages((int) Math.ceil((double) total / size))
                .last(page >= (int) Math.ceil((double) total / size) - 1)
                .build();
    }

    // ─── Món đã ăn ──────────────────────────────────────────────────────────────

    /**
     * Danh sách tất cả món ăn khách đã từng gọi, sắp xếp theo số lượng gọi nhiều nhất.
     * Dùng GROUP BY trực tiếp trong query để không phải load toàn bộ order rồi xử lý Java.
     */
    @Transactional(readOnly = true)
    public List<CustomerDishEatenResponse> getDishesEaten(Integer userId) {
        Customer customer = requireCustomerByUserId(userId);

        return orderRepository.findDishesEatenByCustomerId(customer.getId())
                .stream()
                .map(p -> new CustomerDishEatenResponse(
                        p.getMenuItemId(),
                        p.getName(),
                        p.getImageUrl(),
                        p.getCategoryName(),
                        p.getTotalQuantity(),
                        p.getLastOrderedAt()
                ))
                .toList();
    }

    // ─── Đánh giá ───────────────────────────────────────────────────────────────

    /**
     * Tạo đánh giá mới cho món ăn hoặc chi nhánh.
     *
     * Với MENU_ITEM: bắt buộc phải cung cấp orderItemId để xác minh khách đã gọi món.
     * Service kiểm tra order_item thuộc về đúng khách hàng đang đăng nhập trước khi lưu.
     */
    @Transactional
    public CustomerReviewResponse createReview(Integer userId, CreateReviewRequest request) {
        Customer customer = requireCustomerByUserId(userId);
        ReviewType reviewType = ReviewType.valueOf(request.type());

        Review.ReviewBuilder builder = Review.builder()
                .customer(customer)
                .type(reviewType)
                .rating(request.rating())
                .comment(request.comment())
                .status(ReviewStatus.VISIBLE);

        if (reviewType == ReviewType.MENU_ITEM) {
            if (request.orderItemId() == null) {
                throw new BadRequestException("Đánh giá món ăn phải đi kèm orderItemId để xác minh");
            }
            // Kiểm tra đã đánh giá order_item này chưa
            if (reviewRepository.existsByOrderItemId(request.orderItemId())) {
                throw new ConflictException("Bạn đã đánh giá món này rồi");
            }

            OrderItem orderItem = orderItemRepository.findById(request.orderItemId())
                    .orElseThrow(() -> new NotFoundException("Dòng đơn hàng không tồn tại"));

            // Bảo vệ: chỉ chủ đơn hàng mới được đánh giá
            if (orderItem.getOrder().getCustomer() == null
                    || !orderItem.getOrder().getCustomer().getId().equals(customer.getId())) {
                throw new ForbiddenException("Bạn không có quyền đánh giá món này");
            }

            builder.orderItem(orderItem).menuItem(orderItem.getMenuItem());

        } else { // BRANCH
            if (request.branchId() == null) {
                throw new BadRequestException("Đánh giá chi nhánh phải đi kèm branchId");
            }
            Branch branch = branchRepository.findById(request.branchId())
                    .orElseThrow(() -> new NotFoundException("Chi nhánh không tồn tại"));
            builder.branch(branch);
        }

        Review saved = reviewRepository.save(builder.build());
        return toReviewResponse(saved);
    }

    /**
     * Danh sách đánh giá của khách đang đăng nhập — dùng cho trang "Đánh giá của tôi".
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomerReviewResponse> getMyReviews(Integer userId, int page, int size) {
        Customer customer = requireCustomerByUserId(userId);

        Page<Review> reviewPage = reviewRepository.findByCustomerIdOrderByCreatedAtDesc(
                customer.getId(), PageRequest.of(page, size)
        );

        return PageResponse.<CustomerReviewResponse>builder()
                .content(reviewPage.getContent().stream().map(this::toReviewResponse).toList())
                .page(reviewPage.getNumber())
                .size(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .last(reviewPage.isLast())
                .build();
    }

    /**
     * Đánh giá công khai của một món ăn — ai cũng xem được, không cần đăng nhập.
     * Chỉ trả về đánh giá có status = VISIBLE.
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomerReviewResponse> getMenuItemReviews(Integer menuItemId, int page, int size) {
        Page<Review> reviewPage = reviewRepository.findVisibleByMenuItemId(
                menuItemId, ReviewType.MENU_ITEM, ReviewStatus.VISIBLE, PageRequest.of(page, size)
        );

        return PageResponse.<CustomerReviewResponse>builder()
                .content(reviewPage.getContent().stream().map(this::toReviewResponse).toList())
                .page(reviewPage.getNumber())
                .size(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .last(reviewPage.isLast())
                .build();
    }

    // ─── Coupon ─────────────────────────────────────────────────────────────────

    /**
     * Ví coupon của khách, sắp xếp theo ngày nhận mới nhất.
     * Cờ "usable" tự tính: status = AVAILABLE và endDate chưa hết.
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomerCouponResponse> getMyCoupons(Integer userId, int page, int size) {
        Customer customer = requireCustomerByUserId(userId);

        Page<CustomerCoupon> couponPage = customerCouponRepository.findByCustomerIdWithCoupon(
                customer.getId(), PageRequest.of(page, size)
        );

        return PageResponse.<CustomerCouponResponse>builder()
                .content(couponPage.getContent().stream().map(this::toCouponResponse).toList())
                .page(couponPage.getNumber())
                .size(couponPage.getSize())
                .totalElements(couponPage.getTotalElements())
                .totalPages(couponPage.getTotalPages())
                .last(couponPage.isLast())
                .build();
    }

    // ─── Helper nội bộ ──────────────────────────────────────────────────────────

    /**
     * Resolve userId → Customer CRM.
     * Ném NotFoundException nếu chưa có bản ghi (user cũ trước khi feature ra đời).
     */
    private Customer requireCustomerByUserId(Integer userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy hồ sơ khách hàng. Vui lòng liên hệ nhà hàng để được hỗ trợ."));
    }

    /** Trả 0 nếu loyaltyPoints null (khách mới chưa có điểm). */
    private int safePoints(Integer loyaltyPoints) {
        return loyaltyPoints == null ? 0 : loyaltyPoints;
    }

    /** Map Order entity → CustomerOrderResponse DTO. */
    private CustomerOrderResponse toOrderResponse(Order order, Set<Integer> reviewedOrderItemIds) {
        // Tìm bill PAID của đơn (một đơn có thể có nhiều lần in bill nhưng chỉ 1 cái PAID)
        Bill paidBill = order.getBills().stream()
                .filter(b -> b.getStatus() == BillStatus.PAID)
                .findFirst()
                .orElse(null);

        // Số điểm tích từ đơn này — lấy từ loyalty transaction gắn với bill
        Integer pointsEarned = null;
        if (paidBill != null) {
            var earnTx = customerLoyaltyService.findEarnTransactionByBillId(paidBill.getId());
            if (earnTx != null) pointsEarned = earnTx.getPointsDelta();
        }

        List<CustomerOrderItemResponse> items = order.getOrderItems().stream()
                .map(oi -> toOrderItemResponse(oi, reviewedOrderItemIds.contains(oi.getId())))
                .toList();

        return new CustomerOrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getClosedAt(),
                order.getBranch().getId(),
                order.getBranch().getName(),
                order.getTable() != null ? order.getTable().getTableNumber() : null,
                items,
                paidBill != null ? paidBill.getTotal() : null,
                paidBill != null ? paidBill.getStatus().name() : null,
                pointsEarned
        );
    }

    /** Map OrderItem entity → CustomerOrderItemResponse DTO. */
    private CustomerOrderItemResponse toOrderItemResponse(OrderItem oi, boolean reviewed) {
        var mi = oi.getMenuItem();
        return new CustomerOrderItemResponse(
                oi.getId(),
                mi.getId(),
                mi.getName(),
                mi.getImageUrl(),
                mi.getCategory() != null ? mi.getCategory().getName() : null,
                oi.getQuantity(),
                oi.getPrice(),
                oi.getPrice() != null ? oi.getPrice().multiply(java.math.BigDecimal.valueOf(oi.getQuantity())) : null,
                oi.getStatus().name(),
                oi.getNote(),
                reviewed
        );
    }

    /** Map Review entity → CustomerReviewResponse DTO. */
    private CustomerReviewResponse toReviewResponse(Review review) {
        var mi = review.getMenuItem();
        var branch = review.getBranch();
        return new CustomerReviewResponse(
                review.getId(),
                review.getType().name(),
                mi != null ? mi.getId() : null,
                mi != null ? mi.getName() : null,
                mi != null ? mi.getImageUrl() : null,
                branch != null ? branch.getId() : null,
                branch != null ? branch.getName() : null,
                review.getRating(),
                review.getComment(),
                review.getStatus().name(),
                review.getCreatedAt(),
                review.getCustomer().getName()
        );
    }

    /** Map CustomerCoupon entity → CustomerCouponResponse DTO. */
    private CustomerCouponResponse toCouponResponse(CustomerCoupon cc) {
        var coupon = cc.getCoupon();
        boolean usable = cc.getStatus() == CustomerCouponStatus.AVAILABLE
                && (coupon.getEndDate() == null || coupon.getEndDate().isAfter(LocalDateTime.now()));

        return new CustomerCouponResponse(
                cc.getId(),
                coupon.getCode(),
                coupon.getName(),
                coupon.getDescription(),
                coupon.getDiscountType().name(),
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                coupon.getMaxDiscountAmount(),
                coupon.getEndDate(),
                cc.getStatus().name(),
                cc.getReceivedAt(),
                cc.getUsedAt(),
                usable
        );
    }
}
