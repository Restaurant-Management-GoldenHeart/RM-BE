package org.example.goldenheartrestaurant.modules.inventory.repository;

import org.example.goldenheartrestaurant.modules.inventory.entity.MeasurementUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository của bảng đơn vị tính.
 */
public interface MeasurementUnitRepository extends JpaRepository<MeasurementUnit, Integer> {

    /**
     * Code thường dùng cho định danh kỹ thuật, ví dụ: KG, PCS, BOTTLE.
     */
    Optional<MeasurementUnit> findByCodeIgnoreCase(String code);

    /**
     * Symbol là ký hiệu hiển thị ngắn gọn ngoài UI, ví dụ: kg, quả, chai.
     */
    Optional<MeasurementUnit> findBySymbolIgnoreCase(String symbol);
}
