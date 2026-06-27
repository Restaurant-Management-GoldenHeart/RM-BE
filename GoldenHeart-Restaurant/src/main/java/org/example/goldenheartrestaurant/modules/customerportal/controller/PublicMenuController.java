package org.example.goldenheartrestaurant.modules.customerportal.controller;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.modules.customerportal.dto.response.CustomerReviewResponse;
import org.example.goldenheartrestaurant.modules.customerportal.service.CustomerPortalService;
import org.example.goldenheartrestaurant.modules.menu.dto.response.PopularDishResponse;
import org.example.goldenheartrestaurant.modules.menu.service.PublicMenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint không yêu cầu xác thực — dùng cho homepage và trang menu public.
 *
 * Base path: /api/v1/public
 * Chỉ trả dữ liệu read-only, không cho phép ghi.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicMenuController {

    private final CustomerPortalService customerPortalService;
    private final PublicMenuService publicMenuService;

    /**
     * Lấy danh sách đánh giá VISIBLE của một món ăn.
     * Endpoint này public để khách vãng lai trên homepage cũng xem được.
     */
    @GetMapping("/menu-items/{menuItemId}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<CustomerReviewResponse>>> getMenuItemReviews(
            @PathVariable Integer menuItemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<CustomerReviewResponse>>builder()
                .message("Lấy đánh giá món ăn thành công")
                .data(customerPortalService.getMenuItemReviews(menuItemId, page, size))
                .build());
    }

    /**
     * Danh sách món ăn phổ biến cho homepage.
     * Sắp xếp theo tổng số lượt gọi giảm dần (cộng dồn tất cả chi nhánh).
     * Top 2 mỗi danh mục được gắn cờ bestSeller = true.
     */
    @GetMapping("/menu/popular")
    public ResponseEntity<ApiResponse<List<PopularDishResponse>>> getPopularDishes() {
        return ResponseEntity.ok(ApiResponse.<List<PopularDishResponse>>builder()
                .data(publicMenuService.getPopularDishes())
                .message("Lấy danh sách món phổ biến thành công")
                .build());
    }
}
