package org.example.goldenheartrestaurant.modules.inventory.repository;

import org.example.goldenheartrestaurant.modules.inventory.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Integer> {
}
