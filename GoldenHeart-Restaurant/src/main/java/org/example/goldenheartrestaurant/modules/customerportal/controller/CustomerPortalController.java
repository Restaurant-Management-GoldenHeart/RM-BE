package org.example.goldenheartrestaurant.modules.customerportal.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.customer.dto.response.CustomerLoyaltyTransactionResponse;
import org.example.goldenheartrestaurant.modules.customerportal.dto.request.CreateReviewRequest;
import org.example.goldenheartrestaurant.modules.customerportal.dto.request.UpdateCustomerProfileRequest;
import org.example.goldenheartrestaurant.modules.customerportal.dto.response.*;
import org.example.goldenheartrestaurant.modules.customerportal.service.CustomerPortalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller của Customer Portal — tất cả endpoint đều yêu cầu role CUSTOMER.
 *
 * Base path: /api/v1/me
 * Mỗi endpoint lấy userId từ JWT đã được xác thực trong SecurityContext,
 * không nhận customerId từ path variable để tránh lỗ hổng IDOR
 * (khách A không thể xem dữ liệu của khách B bằng cách đổi ID trên URL).
 */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Secured("ROLE_CUSTOMER")
public class CustomerPortalController {

    private final CustomerPortalService customerPortalService;

    // ─── Profile ────────────────────────────────────────────────────────────────

    /**
     * Lấy thông tin hồ sơ của khách đang đăng nhập.
     * Trả kèm tier hiện tại và tier tiếp theo để FE hiển thị thanh tiến trình điểm.
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.<CustomerProfileResponse>builder()
                .message("Lấy hồ sơ thành công")
                .data(customerPortalService.getProfile(currentUser.getUserId()))
                .build());
    }

    /**
     * Cập nhật thông tin cá nhân — chỉ cho phép sửa name, phone, address, dateOfBirth, gender.
     * Email và điểm tích luỹ được bảo vệ, không thể sửa qua endpoint này.
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UpdateCustomerProfileRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.<CustomerProfileResponse>builder()
                .message("Cập nhật hồ sơ thành công")
                .data(customerPortalService.updateProfile(currentUser.getUserId(), request))
                .build());
    }

    // ─── Loyalty ────────────────────────────────────────────────────────────────

    /**
     * Lịch sử tích/trừ điểm tích luỹ, phân trang.
     * Mỗi bản ghi ghi rõ: loại giao dịch, delta điểm, số dư trước và sau.
     */
    @GetMapping("/loyalty/transactions")
    public ResponseEntity<ApiResponse<PageResponse<CustomerLoyaltyTransactionResponse>>> getLoyaltyTransactions(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<CustomerLoyaltyTransactionResponse>>builder()
                .message("Lấy lịch sử điểm thành công")
                .data(customerPortalService.getLoyaltyTransactions(currentUser.getUserId(), page, size))
                .build());
    }

    // ─── Lịch sử đơn hàng ───────────────────────────────────────────────────────

    /**
     * Lịch sử đơn hàng của khách, phân trang, mới nhất trước.
     * Mỗi đơn có danh sách món và cờ "reviewed" cho từng dòng OrderItem.
     */
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<PageResponse<CustomerOrderResponse>>> getOrderHistory(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<CustomerOrderResponse>>builder()
                .message("Lấy lịch sử đơn hàng thành công")
                .data(customerPortalService.getOrderHistory(currentUser.getUserId(), page, size))
                .build());
    }

    // ─── Món đã ăn ──────────────────────────────────────────────────────────────

    /**
     * Toàn bộ món ăn khách đã gọi qua mọi đơn hàng, sắp xếp theo số lượng.
     * Không phân trang vì thường không quá nhiều (dưới 100 món unique/khách).
     */
    @GetMapping("/dishes-eaten")
    public ResponseEntity<ApiResponse<List<CustomerDishEatenResponse>>> getDishesEaten(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.<List<CustomerDishEatenResponse>>builder()
                .message("Lấy danh sách món đã ăn thành công")
                .data(customerPortalService.getDishesEaten(currentUser.getUserId()))
                .build());
    }

    // ─── Đánh giá ───────────────────────────────────────────────────────────────

    /**
     * Tạo đánh giá mới cho món ăn hoặc chi nhánh.
     * Với MENU_ITEM: bắt buộc cung cấp orderItemId để xác minh đã gọi món.
     */
    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<CustomerReviewResponse>> createReview(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CustomerReviewResponse>builder()
                        .message("Đánh giá của bạn đã được gửi thành công")
                        .data(customerPortalService.createReview(currentUser.getUserId(), request))
                        .build());
    }

    /**
     * Lấy danh sách đánh giá của khách đang đăng nhập ("Đánh giá của tôi").
     */
    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<PageResponse<CustomerReviewResponse>>> getMyReviews(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<CustomerReviewResponse>>builder()
                .message("Lấy danh sách đánh giá thành công")
                .data(customerPortalService.getMyReviews(currentUser.getUserId(), page, size))
                .build());
    }

    // ─── Coupon ─────────────────────────────────────────────────────────────────

    /**
     * Ví coupon của khách — bao gồm coupon khả dụng, đã dùng và hết hạn.
     */
    @GetMapping("/coupons")
    public ResponseEntity<ApiResponse<PageResponse<CustomerCouponResponse>>> getMyCoupons(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<CustomerCouponResponse>>builder()
                .message("Lấy ví coupon thành công")
                .data(customerPortalService.getMyCoupons(currentUser.getUserId(), page, size))
                .build());
    }
}
