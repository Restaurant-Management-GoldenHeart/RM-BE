package org.example.goldenheartrestaurant.modules.inventory.repository;

import org.example.goldenheartrestaurant.modules.inventory.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {
}
