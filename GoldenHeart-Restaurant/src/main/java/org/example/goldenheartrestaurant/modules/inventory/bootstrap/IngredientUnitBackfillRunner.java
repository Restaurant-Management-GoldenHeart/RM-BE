package org.example.goldenheartrestaurant.modules.inventory.bootstrap;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.modules.inventory.entity.Ingredient;
import org.example.goldenheartrestaurant.modules.inventory.entity.MeasurementUnit;
import org.example.goldenheartrestaurant.modules.inventory.repository.IngredientRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.MeasurementUnitRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(2)
@RequiredArgsConstructor
public class IngredientUnitBackfillRunner implements ApplicationRunner {

    private final IngredientRepository ingredientRepository;
    private final MeasurementUnitRepository measurementUnitRepository;

    @Override
    public void run(ApplicationArguments args) {
        for (Ingredient ingredient : ingredientRepository.findByMeasurementUnitIsNull()) {
            if (!StringUtils.hasText(ingredient.getLegacyUnit())) {
                continue;
            }

            measurementUnitRepository.findBySymbolIgnoreCase(ingredient.getLegacyUnit().trim())
                    .ifPresent(unit -> {
                        ingredient.setMeasurementUnit(unit);
                        ingredient.setLegacyUnit(unit.getSymbol());
                        ingredientRepository.save(ingredient);
                    });
        }
    }
}
