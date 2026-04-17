package org.example.goldenheartrestaurant.modules.restaurant.repository;

import org.example.goldenheartrestaurant.modules.restaurant.entity.DiningArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiningAreaRepository extends JpaRepository<DiningArea, Integer> {

    Optional<DiningArea> findByIdAndBranch_Id(Integer id, Integer branchId);
}
