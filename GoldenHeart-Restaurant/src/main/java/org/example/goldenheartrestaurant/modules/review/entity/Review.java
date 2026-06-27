package org.example.goldenheartrestaurant.modules.review.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.goldenheartrestaurant.modules.customer.entity.Customer;
import org.example.goldenheartrestaurant.modules.menu.entity.MenuItem;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItem;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;

import java.time.LocalDateTime;

/**
 * Lưu đánh giá của khách hàng cho món ăn hoặc chi nhánh.
 *
 * Thiết kế anti-fake: trường orderItem liên kết đến đơn hàng thực tế.
 * Service sẽ kiểm tra orderItem.order.customer == review.customer trước khi lưu.
 * Mỗi order_item chỉ được đánh giá một lần (unique constraint).
 */
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                // Mỗi order_item_id chỉ có thể được đánh giá một lần duy nhất.
                @UniqueConstraint(name = "uk_reviews_order_item_id", columnNames = "order_item_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Khách hàng thực hiện đánh giá. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** Loại đối tượng được đánh giá: MENU_ITEM hoặc BRANCH. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewType type;

    /** Nullable: chỉ có giá trị khi type = MENU_ITEM. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    /** Nullable: chỉ có giá trị khi type = BRANCH. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    /** Điểm từ 1 đến 5 sao. */
    @Column(nullable = false)
    private Integer rating;

    /** Nội dung nhận xét — không bắt buộc. */
    @Column(length = 2000)
    private String comment;

    /**
     * Dòng OrderItem chứng minh khách đã gọi món này.
     * Nullable vì đánh giá chi nhánh (BRANCH) không cần order_item cụ thể.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.VISIBLE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
