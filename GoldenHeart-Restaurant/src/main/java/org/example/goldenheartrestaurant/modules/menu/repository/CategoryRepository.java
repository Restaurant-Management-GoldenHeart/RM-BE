package org.example.goldenheartrestaurant.modules.menu.repository;

import org.example.goldenheartrestaurant.modules.menu.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cơ bản của danh mục món ăn.
 *
 * Hiện tại Category chủ yếu đóng vai trò dữ liệu tham chiếu cho MenuItem,
 * nên chỉ cần các hàm CRUD mặc định của JpaRepository là đủ.
 */
public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
