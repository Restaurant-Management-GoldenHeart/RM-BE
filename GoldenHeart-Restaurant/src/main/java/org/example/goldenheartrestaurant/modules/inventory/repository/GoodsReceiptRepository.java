package org.example.goldenheartrestaurant.modules.inventory.repository;

import org.example.goldenheartrestaurant.modules.inventory.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Integer> {

    boolean existsByReceiptCode(String receiptCode);
}