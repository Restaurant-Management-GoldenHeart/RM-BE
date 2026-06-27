package org.example.goldenheartrestaurant.modules.review.entity;

/**
 * Trạng thái hiển thị của đánh giá.
 * VISIBLE  — đánh giá được công khai cho mọi khách hàng xem.
 * HIDDEN   — bị ẩn bởi admin (spam, vi phạm nội quy).
 */
public enum ReviewStatus {
    VISIBLE,
    HIDDEN
}
