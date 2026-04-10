package org.example.goldenheartrestaurant.modules.menu.repository;

import org.example.goldenheartrestaurant.modules.menu.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeRepository extends JpaRepository<Recipe, Integer> {

    @Modifying
    @Query("delete from Recipe r where r.menuItem.id = :menuItemId")
    void deleteByMenuItemId(@Param("menuItemId") Integer menuItemId);
}
