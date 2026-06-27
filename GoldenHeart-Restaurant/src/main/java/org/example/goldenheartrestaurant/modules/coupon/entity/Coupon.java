package org.example.goldenheartrestaurant.modules.coupon.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.goldenheartrestaurant.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Coupon giảm giá do admin tạo và phát cho khách hàng.
 *
 * Quy tắc áp dụng:
 * - usageLimit null  → không giới hạn tổng số lượt dùng trên toàn hệ thống.
 * - perCustomerLimit → mỗi khách chỉ được nhận/dùng tối đa n lần.
 * - maxDiscountAmount → cap giảm giá khi dùng PERCENTAGE (vd: giảm 20% nhưng tối đa 100k).
 */
@Entity
@Table(
        name = "coupons",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_coupons_code", columnNames = "code")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Mã coupon duy nhất, khách nhập để áp dụng. */
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    /** Tên hiển thị, vd: "Mừng sinh nhật", "Khách hàng mới". */
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    /** Giá trị giảm: % hoặc số tiền tuỳ discountType. */
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    /** Giá trị đơn hàng tối thiểu để áp coupon. Null = không giới hạn. */
    @Column(name = "min_order_amount", precision = 12, scale = 2)
    private BigDecimal minOrderAmount;

    /** Số tiền giảm tối đa khi dùng PERCENTAGE. Null = không cap. */
    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    /** Tổng số lượt dùng trên toàn hệ thống. Null = không giới hạn. */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    /** Số lần đã được dùng thực tế. */
    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

    /** Số lần tối đa mỗi khách được nhận coupon này. */
    @Column(name = "per_customer_limit", nullable = false)
    @Builder.Default
    private Integer perCustomerLimit = 1;

    /** Thời điểm coupon bắt đầu có hiệu lực. */
    @Column(name = "start_date")
    private LocalDateTime startDate;

    /** Thời điểm coupon hết hạn. */
    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CouponStatus status = CouponStatus.ACTIVE;
}
