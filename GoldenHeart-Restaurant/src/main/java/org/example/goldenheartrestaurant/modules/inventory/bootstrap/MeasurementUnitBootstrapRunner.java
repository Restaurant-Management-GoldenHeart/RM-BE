package org.example.goldenheartrestaurant.modules.inventory.bootstrap;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.modules.inventory.entity.MeasurementUnit;
import org.example.goldenheartrestaurant.modules.inventory.repository.MeasurementUnitRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
public class MeasurementUnitBootstrapRunner implements ApplicationRunner {

    private final MeasurementUnitRepository measurementUnitRepository;

    @Override
    public void run(ApplicationArguments args) {
        ensureUnit("KG", "Kilogram", "kg", "Don vi ton kho cho thit, bot, rau cu...");
        ensureUnit("PIECE", "Piece", "piece", "Don vi dem theo tung cai, qua, chai...");
        ensureUnit("LITER", "Liter", "l", "Don vi ton kho theo lit");
        ensureUnit("GRAM", "Gram", "g", "Don vi ton kho nho hon kilogram");
        ensureUnit("MILLILITER", "Milliliter", "ml", "Don vi ton kho nho hon liter");
        ensureUnit("PCS", "Cai/Chiec", "pcs", "Don vi dem thong dung");
        ensureUnit("BOX", "Hop", "box", "Don vi mua/dong goi theo hop");
        ensureUnit("BUNCH", "Bo", "bunch", "Don vi mua theo bo rau/cu");
        ensureUnit("JAR", "Hu", "jar", "Don vi mua theo hu/lo");
        ensureUnit("BOTTLE", "Chai", "bottle", "Don vi mua theo chai");
        ensureUnit("CASE", "Thung", "case", "Don vi mua theo thung/kien");
        ensureUnit("BAG", "Bao/Tui", "bag", "Don vi mua theo bao/tui");
        ensureUnit("CAN", "Lon", "can", "Don vi mua theo lon");
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
