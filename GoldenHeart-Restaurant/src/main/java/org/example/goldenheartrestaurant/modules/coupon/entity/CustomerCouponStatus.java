package org.example.goldenheartrestaurant.modules.coupon.entity;

/** Trạng thái của một coupon trong ví khách hàng. */
public enum CustomerCouponStatus {
    /** Chưa dùng, còn hạn sử dụng. */
    AVAILABLE,
    /** Đã dùng để thanh toán một hoá đơn. */
    USED,
    /** Hết hạn mà chưa dùng. */
    EXPIRED
}
