package org.example.goldenheartrestaurant.modules.customerportal.dto.response;

import java.time.LocalDateTime;

/** Một đánh giá của khách — dùng cho cả trang "Đánh giá của tôi" và public review của món. */
public record CustomerReviewResponse(
        Integer reviewId,
        String type,
        Integer menuItemId,
        String menuItemName,
        String menuItemImageUrl,
        Integer branchId,
        String branchName,
        Integer rating,
        String comment,
        String status,
        LocalDateTime createdAt,

        // Tên người đánh giá (hiển thị khi xem public review)
        String reviewerName
) {
}
