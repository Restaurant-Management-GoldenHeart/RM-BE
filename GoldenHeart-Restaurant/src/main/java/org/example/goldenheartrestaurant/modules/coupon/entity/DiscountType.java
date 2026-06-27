package org.example.goldenheartrestaurant.modules.coupon.entity;

/** Kiểu giảm giá của coupon: theo phần trăm hoặc số tiền cố định. */
public enum DiscountType {
    /** Giảm theo % tổng hoá đơn (vd: 10%). */
    PERCENTAGE,
    /** Giảm số tiền cố định (vd: 50.000đ). */
    FIXED_AMOUNT
}
