package org.example.goldenheartrestaurant.modules.inventory.bootstrap;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.modules.inventory.entity.MeasurementUnit;
import org.example.goldenheartrestaurant.modules.inventory.repository.MeasurementUnitRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
public class MeasurementUnitBootstrapRunner implements ApplicationRunner {

    private final MeasurementUnitRepository measurementUnitRepository;

    @Override
    public void run(ApplicationArguments args) {
        ensureUnit("KG", "Kilogram", "kg", "Don vi tinh cho thit, bot, rau cu...");
        ensureUnit("PIECE", "Piece", "piece", "Don vi tinh theo tung cai, qua, chai...");
        ensureUnit("LITER", "Liter", "l", "Don vi tinh theo lit");
        ensureUnit("GRAM", "Gram", "g", "Don vi tinh nho hon kilogram");
        ensureUnit("MILLILITER", "Milliliter", "ml", "Don vi tinh nho hon liter");
    }

    private void ensureUnit(String code, String name, String symbol, String description) {
        if (measurementUnitRepository.findByCodeIgnoreCase(code).isPresent()) {
            return;
        }

        measurementUnitRepository.save(
                MeasurementUnit.builder()
                        .code(code)
                        .name(name)
                        .symbol(symbol)
                        .description(description)
                        .build()
        );
    }
}
