package org.example.goldenheartrestaurant.modules.customerportal.dto.request;

import jakarta.validation.constraints.*;

/**
 * Yêu cầu tạo đánh giá từ khách hàng.
 *
 * Quy tắc validate:
 * - type bắt buộc: MENU_ITEM hoặc BRANCH.
 * - Nếu type = MENU_ITEM thì orderItemId bắt buộc (chứng minh đã gọi món).
 * - Nếu type = BRANCH thì branchId bắt buộc.
 * - rating từ 1 đến 5.
 * - comment không bắt buộc nhưng giới hạn 2000 ký tự.
 */
public record CreateReviewRequest(

        @NotBlank(message = "Loại đánh giá không được để trống")
        @Pattern(regexp = "^(MENU_ITEM|BRANCH)$", message = "Loại đánh giá phải là MENU_ITEM hoặc BRANCH")
        String type,

        /** Bắt buộc khi type = MENU_ITEM. */
        Integer orderItemId,

        /** Bắt buộc khi type = BRANCH. */
        Integer branchId,

        @NotNull(message = "Điểm đánh giá không được để trống")
        @Min(value = 1, message = "Điểm tối thiểu là 1")
        @Max(value = 5, message = "Điểm tối đa là 5")
        Integer rating,

        @Size(max = 2000, message = "Nội dung đánh giá không được vượt quá 2000 ký tự")
        String comment
) {
}
