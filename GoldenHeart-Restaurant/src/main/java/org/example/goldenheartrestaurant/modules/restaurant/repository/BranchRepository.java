package org.example.goldenheartrestaurant.modules.restaurant.repository;

import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cơ bản của Branch.
 *
 * Branch là dữ liệu tham chiếu xuất hiện ở nhiều module:
 * menu, inventory, order, employee profile...
 * Nên hiện tại chỉ cần CRUD chuẩn để các service khác tra cứu.
 */
public interface BranchRepository extends JpaRepository<Branch, Integer> {
}
