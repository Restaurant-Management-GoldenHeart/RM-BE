package org.example.goldenheartrestaurant.modules.inventory.repository;

import org.example.goldenheartrestaurant.modules.inventory.entity.InventoryActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository log audit của inventory.
 *
 * Dùng để xem lịch sử ai đã tạo / sửa / xóa mềm một inventory item.
 */
public interface InventoryActionLogRepository extends JpaRepository<InventoryActionLog, Integer> {

    /**
     * Trả lịch sử thao tác của một inventory item theo thứ tự mới nhất trước,
     * để FE có thể hiển thị timeline thay đổi dễ đọc hơn.
     */
    Page<InventoryActionLog> findByInventoryIdOrderByOccurredAtDesc(Integer inventoryId, Pageable pageable);
}
