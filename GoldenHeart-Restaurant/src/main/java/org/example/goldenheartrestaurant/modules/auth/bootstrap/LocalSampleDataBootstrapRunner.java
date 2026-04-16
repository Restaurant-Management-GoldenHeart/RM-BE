package org.example.goldenheartrestaurant.modules.auth.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.goldenheartrestaurant.modules.identity.entity.User;
import org.example.goldenheartrestaurant.modules.identity.repository.UserRepository;
import org.example.goldenheartrestaurant.modules.inventory.entity.Ingredient;
import org.example.goldenheartrestaurant.modules.inventory.entity.Inventory;
import org.example.goldenheartrestaurant.modules.inventory.entity.MeasurementUnit;
import org.example.goldenheartrestaurant.modules.inventory.repository.IngredientRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.InventoryRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.MeasurementUnitRepository;
import org.example.goldenheartrestaurant.modules.menu.entity.Category;
import org.example.goldenheartrestaurant.modules.menu.entity.MenuItem;
import org.example.goldenheartrestaurant.modules.menu.entity.MenuItemStatus;
import org.example.goldenheartrestaurant.modules.menu.entity.Recipe;
import org.example.goldenheartrestaurant.modules.menu.repository.CategoryRepository;
import org.example.goldenheartrestaurant.modules.menu.repository.MenuItemRepository;
import org.example.goldenheartrestaurant.modules.order.entity.Order;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItem;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItemStatus;
import org.example.goldenheartrestaurant.modules.order.entity.OrderStatus;
import org.example.goldenheartrestaurant.modules.order.repository.OrderItemRepository;
import org.example.goldenheartrestaurant.modules.order.repository.OrderRepository;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Restaurant;
import org.example.goldenheartrestaurant.modules.restaurant.repository.BranchRepository;
import org.example.goldenheartrestaurant.modules.restaurant.repository.RestaurantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.bootstrap.sample-data.enabled", havingValue = "true", matchIfMissing = false)
@org.springframework.core.annotation.Order(3)
@RequiredArgsConstructor
public class LocalSampleDataBootstrapRunner implements ApplicationRunner {

    private final RestaurantRepository restaurantRepository;
    private final BranchRepository branchRepository;
    private final CategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;
    private final InventoryRepository inventoryRepository;
    private final MeasurementUnitRepository measurementUnitRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Value("${app.bootstrap.sample-data.enabled:false}")
    private boolean sampleDataEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!sampleDataEnabled) {
            return;
        }

        // Skip seeding when any baseline business data already exists to keep startup idempotent
        // even if DB was partially seeded by SQL scripts.
        if (restaurantRepository.count() > 0
                || branchRepository.count() > 0
                || categoryRepository.count() > 0
                || ingredientRepository.count() > 0
                || inventoryRepository.count() > 0
                || menuItemRepository.count() > 0
                || orderItemRepository.count() > 0) {
            return;
        }

        User admin = userRepository.findActiveAuthUserByUsername("admin")
                .orElse(null);

        if (admin == null) {
            log.warn("Skipping sample data bootstrap because admin user is missing");
            return;
        }

        MeasurementUnit kgUnit = measurementUnitRepository.findByCodeIgnoreCase("KG")
                .orElseThrow(() -> new IllegalStateException("KG measurement unit is missing"));

        Restaurant restaurant = restaurantRepository.save(
                Restaurant.builder()
                        .name("GoldenHeart Restaurant")
                        .address("1 Nguyen Hue, District 1, Ho Chi Minh City")
                        .phone("0900000001")
                        .build()
        );

        Branch branch = branchRepository.save(
                Branch.builder()
                        .restaurant(restaurant)
                        .name("GoldenHeart D1")
                        .address("1 Nguyen Hue, District 1, Ho Chi Minh City")
                        .phone("0900000002")
                        .build()
        );

        Category category = categoryRepository.save(
                Category.builder()
                        .name("Pho")
                        .description("Vietnamese noodle soup")
                        .build()
        );

        Ingredient beef = ingredientRepository.save(
                Ingredient.builder()
                        .name("Beef")
                        .measurementUnit(kgUnit)
                        .legacyUnit(kgUnit.getSymbol())
                        .build()
        );

        Ingredient noodle = ingredientRepository.save(
                Ingredient.builder()
                        .name("Rice Noodle")
                        .measurementUnit(kgUnit)
                        .legacyUnit(kgUnit.getSymbol())
                        .build()
        );

        inventoryRepository.save(
                Inventory.builder()
                        .branch(branch)
                        .ingredient(beef)
                        .quantity(new BigDecimal("10.00"))
                        .minStockLevel(new BigDecimal("2.00"))
                        .reorderLevel(new BigDecimal("3.00"))
                        .averageUnitCost(new BigDecimal("220000.00"))
                        .lastReceiptAt(LocalDateTime.now())
                        .build()
        );

        inventoryRepository.save(
                Inventory.builder()
                        .branch(branch)
                        .ingredient(noodle)
                        .quantity(new BigDecimal("20.00"))
                        .minStockLevel(new BigDecimal("3.00"))
                        .reorderLevel(new BigDecimal("5.00"))
                        .averageUnitCost(new BigDecimal("45000.00"))
                        .lastReceiptAt(LocalDateTime.now())
                        .build()
        );

        MenuItem menuItem = MenuItem.builder()
                .branch(branch)
                .category(category)
                .name("Pho Bo Tai")
                .description("Pho bo tai sample for local testing")
                .price(new BigDecimal("85000.00"))
                .status(MenuItemStatus.AVAILABLE)
                .build();

        menuItem.getRecipes().add(
                Recipe.builder()
                        .menuItem(menuItem)
                        .ingredient(beef)
                        .quantity(new BigDecimal("0.20"))
                        .build()
        );
        menuItem.getRecipes().add(
                Recipe.builder()
                        .menuItem(menuItem)
                        .ingredient(noodle)
                        .quantity(new BigDecimal("0.15"))
                        .build()
        );

        MenuItem savedMenuItem = menuItemRepository.save(menuItem);

        Order order = orderRepository.save(
                Order.builder()
                        .branch(branch)
                        .createdBy(admin)
                        .status(OrderStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        OrderItem orderItem = orderItemRepository.save(
                OrderItem.builder()
                        .order(order)
                        .menuItem(savedMenuItem)
                        .quantity(2)
                        .price(savedMenuItem.getPrice())
                        .status(OrderItemStatus.PENDING)
                        .note("Sample pending order item for kitchen API")
                        .build()
        );

        log.info("Sample data seeded: branchId={}, categoryId={}, beefIngredientId={}, noodleIngredientId={}, menuItemId={}, orderId={}, orderItemId={}",
                branch.getId(),
                category.getId(),
                beef.getId(),
                noodle.getId(),
                savedMenuItem.getId(),
                order.getId(),
                orderItem.getId()
        );
    }
}
