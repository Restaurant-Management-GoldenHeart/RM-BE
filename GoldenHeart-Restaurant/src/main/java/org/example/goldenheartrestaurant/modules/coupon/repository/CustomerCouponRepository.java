package org.example.goldenheartrestaurant.modules.coupon.repository;

import org.example.goldenheartrestaurant.modules.coupon.entity.CustomerCoupon;
import org.example.goldenheartrestaurant.modules.coupon.entity.CustomerCouponStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository ví coupon của khách hàng.
 *
 * Tất cả query đều fetch sẵn coupon để tránh N+1 khi render danh sách ví.
 */
public interface CustomerCouponRepository extends JpaRepository<CustomerCoupon, Integer> {

    /**
     * Lấy toàn bộ ví coupon của khách, mới nhất trước.
     * Fetch coupon ngay để service không phải load lazy thêm lần nữa.
     */
    @Query("""
            SELECT cc FROM CustomerCoupon cc
            JOIN FETCH cc.coupon cp
            WHERE cc.customer.id = :customerId
            ORDER BY cc.receivedAt DESC
            """)
    Page<CustomerCoupon> findByCustomerIdWithCoupon(
            @Param("customerId") Integer customerId,
            Pageable pageable
    );

    /** Đếm số lần khách đã nhận một coupon cụ thể (để kiểm tra perCustomerLimit). */
    long countByCustomerIdAndCouponId(Integer customerId, Integer couponId);

    /** Kiểm tra khách có coupon ở trạng thái AVAILABLE chưa. */
    boolean existsByCustomerIdAndCouponIdAndStatus(
            Integer customerId, Integer couponId, CustomerCouponStatus status
    );
}
