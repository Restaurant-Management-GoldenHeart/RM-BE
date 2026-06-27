package org.example.goldenheartrestaurant.modules.coupon.entity;

/** Trạng thái vòng đời của coupon (admin quản lý). */
public enum CouponStatus {
    /** Đang hoạt động, có thể phát cho khách hàng. */
    ACTIVE,
    /** Tạm ngừng — không thể dùng nhưng chưa xoá. */
    INACTIVE
}
