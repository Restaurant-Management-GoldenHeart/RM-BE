package org.example.goldenheartrestaurant.modules.order.repository;

import org.example.goldenheartrestaurant.modules.customerportal.repository.DishEatenProjection;
import org.example.goldenheartrestaurant.modules.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository cơ bản của Order.
 *
 * Hiện tại luồng order phức tạp chủ yếu đi qua OrderItemRepository,
 * còn OrderRepository vẫn đủ dùng với CRUD mặc định.
 */
public interface OrderRepository extends JpaRepository<Order, Integer> {

    boolean existsByTable_Id(Integer tableId);

    boolean existsByBranch_Id(Integer branchId);

    @Query("""
            select distinct o
            from Order o
            join fetch o.branch b
            left join fetch o.table t
            left join fetch o.customer c
            left join fetch o.orderItems oi
            left join fetch oi.menuItem mi
            where o.id = :orderId
            """)
    Optional<Order> findDetailById(@Param("orderId") Integer orderId);

    @Query("""
            select distinct o
            from Order o
            join fetch o.branch b
            join fetch o.table t
            left join fetch o.customer c
            left join fetch o.orderItems oi
            left join fetch oi.menuItem mi
            where t.id = :tableId
              and o.closedAt is null
              and o.status <> org.example.goldenheartrestaurant.modules.order.entity.OrderStatus.CANCELLED
            order by o.createdAt desc
            """)
    List<Order> findActiveOrdersByTableId(@Param("tableId") Integer tableId);

    @Query("""
            select count(o)
            from Order o
            where (:branchId is null or o.branch.id = :branchId)
              and o.closedAt is null
              and o.status <> org.example.goldenheartrestaurant.modules.order.entity.OrderStatus.CANCELLED
            """)
    long countActiveOrdersForReport(@Param("branchId") Integer branchId);

    /**
     * Lịch sử đơn hàng của một khách hàng (customer portal).
     * Fetch sẵn branch, table, orderItems và menuItem để tránh N+1 khi render.
     * Dùng PAGE trên ID trước, sau đó JOIN FETCH để đảm bảo phân trang chính xác.
     */
    @Query("""
            SELECT DISTINCT o FROM Order o
            JOIN FETCH o.branch b
            LEFT JOIN FETCH o.table t
            LEFT JOIN FETCH o.orderItems oi
            LEFT JOIN FETCH oi.menuItem mi
            LEFT JOIN FETCH mi.category cat
            WHERE o.customer.id = :customerId
            ORDER BY o.createdAt DESC
            """)
    List<Order> findByCustomerIdForPortal(@Param("customerId") Integer customerId, Pageable pageable);

    /** Đếm tổng số đơn hàng của khách — dùng riêng để tính phân trang. */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.id = :customerId")
    long countByCustomerId(@Param("customerId") Integer customerId);

    /**
     * Thống kê "Món tôi đã ăn" — gộp theo TÊN MÓN, sắp xếp theo số lượng gọi nhiều nhất.
     *
     * <p><b>Tại sao GROUP BY tên thay vì ID?</b><br>
     * Mỗi chi nhánh quản lý bản sao menu riêng, nên cùng một món (ví dụ "Bò lúc lắc")
     * có thể tồn tại dưới nhiều {@code menu_item.id} khác nhau — một ID cho mỗi chi nhánh.
     * Nếu GROUP BY id, khách hàng đặt món đó ở 2 chi nhánh sẽ thấy 2 thẻ riêng biệt
     * thay vì 1 thẻ tổng hợp duy nhất.
     *
     * <p><b>Tiền đề nghiệp vụ:</b><br>
     * Tên món là định danh duy nhất về mặt nghiệp vụ — không có 2 món khác nhau
     * về bản chất mà lại được đặt trùng tên. Vì vậy GROUP BY name là an toàn
     * và phản ánh đúng ý nghĩa "đây là cùng 1 món, khách đã ăn ở nhiều chi nhánh".
     *
     * <p><b>Các cột không nằm trong GROUP BY phải dùng aggregate function:</b>
     * <ul>
     *   <li>{@code menuItemId} — {@code MIN(id)}: lấy 1 ID đại diện bất kỳ;
     *       chỉ dùng làm {@code key} phân biệt dòng trong danh sách, không mang nghĩa logic.</li>
     *   <li>{@code imageUrl} — {@code MIN(imageUrl)}: ưu tiên ảnh đầu tiên khác null
     *       (MIN bỏ qua null trong SQL). Các chi nhánh thường dùng chung ảnh món.</li>
     *   <li>{@code categoryName} — {@code MIN(categoryName)}: lấy danh mục đại diện;
     *       cùng tên món thì category luôn đồng nhất giữa các chi nhánh.</li>
     * </ul>
     *
     * <p>Bỏ qua đơn hàng CANCELLED để chỉ tính những lần thực sự được phục vụ.
     */
    @Query("""
            SELECT MIN(mi.id)       AS menuItemId,
                   mi.name          AS name,
                   MIN(mi.imageUrl) AS imageUrl,
                   MIN(cat.name)    AS categoryName,
                   SUM(oi.quantity) AS totalQuantity,
                   MAX(o.createdAt) AS lastOrderedAt
            FROM OrderItem oi
            JOIN oi.order o
            JOIN oi.menuItem mi
            JOIN mi.category cat
            WHERE o.customer.id = :customerId
              AND o.status <> org.example.goldenheartrestaurant.modules.order.entity.OrderStatus.CANCELLED
            GROUP BY mi.name
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<DishEatenProjection> findDishesEatenByCustomerId(@Param("customerId") Integer customerId);
}
