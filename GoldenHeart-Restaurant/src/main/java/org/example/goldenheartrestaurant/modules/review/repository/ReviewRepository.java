package org.example.goldenheartrestaurant.modules.review.repository;

import org.example.goldenheartrestaurant.modules.review.entity.Review;
import org.example.goldenheartrestaurant.modules.review.entity.ReviewStatus;
import org.example.goldenheartrestaurant.modules.review.entity.ReviewType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository của Review.
 *
 * Tách biệt rõ hai hướng query:
 * - Phía khách hàng: xem đánh giá của bản thân, kiểm tra đã review chưa.
 * - Phía public: xem tất cả đánh giá VISIBLE của một món ăn hoặc chi nhánh.
 */
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    /** Lấy tất cả đánh giá của một khách hàng, sắp xếp mới nhất trước. */
    Page<Review> findByCustomerIdOrderByCreatedAtDesc(Integer customerId, Pageable pageable);

    /**
     * Kiểm tra order_item này đã được đánh giá chưa.
     * Dùng để ngăn khách đánh giá trùng cùng một lần gọi món.
     */
    boolean existsByOrderItemId(Integer orderItemId);

    /** Lấy đánh giá theo order_item — xem lại review đã viết. */
    Optional<Review> findByOrderItemId(Integer orderItemId);

    /**
     * Lấy tất cả đánh giá VISIBLE của một món ăn (public).
     * Fetch trước customer để tránh N+1 khi render danh sách.
     */
    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.customer c
            WHERE r.menuItem.id = :menuItemId
              AND r.type = :type
              AND r.status = :status
            ORDER BY r.createdAt DESC
            """)
    Page<Review> findVisibleByMenuItemId(
            @Param("menuItemId") Integer menuItemId,
            @Param("type") ReviewType type,
            @Param("status") ReviewStatus status,
            Pageable pageable
    );

    /**
     * Lấy thống kê tổng hợp (avg rating, tổng lượt đánh giá) cho một món.
     * Trả về mảng [avgRating, count].
     */
    @Query("""
            SELECT AVG(r.rating), COUNT(r)
            FROM Review r
            WHERE r.menuItem.id = :menuItemId
              AND r.type = 'MENU_ITEM'
              AND r.status = 'VISIBLE'
            """)
    List<Object[]> findRatingStatsByMenuItemId(@Param("menuItemId") Integer menuItemId);
}
