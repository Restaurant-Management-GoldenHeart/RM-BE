package org.example.goldenheartrestaurant.modules.coupon.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.goldenheartrestaurant.modules.customer.entity.Customer;

import java.time.LocalDateTime;

/**
 * Bảng trung gian lưu coupon trong ví của từng khách hàng.
 *
 * Mỗi bản ghi đại diện cho một lượt phát coupon: một khách có thể nhận
 * cùng một coupon nhiều lần nếu perCustomerLimit > 1.
 */
@Entity
@Table(name = "customer_coupons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CustomerCouponStatus status = CustomerCouponStatus.AVAILABLE;

    /** Thời điểm coupon được phát vào ví khách. */
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    /** Thời điểm khách dùng coupon — null nếu chưa dùng. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /** ID hoá đơn đã áp dụng coupon — null nếu chưa dùng. */
    @Column(name = "used_bill_id")
    private Integer usedBillId;

    @PrePersist
    void onCreate() {
        if (receivedAt == null) receivedAt = LocalDateTime.now();
    }
}
