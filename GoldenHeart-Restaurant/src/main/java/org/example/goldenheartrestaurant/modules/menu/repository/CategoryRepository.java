package org.example.goldenheartrestaurant.modules.menu.repository;

import org.example.goldenheartrestaurant.modules.menu.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
