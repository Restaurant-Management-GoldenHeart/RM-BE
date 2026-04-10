package org.example.goldenheartrestaurant.modules.menu.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.modules.inventory.entity.Ingredient;
import org.example.goldenheartrestaurant.modules.inventory.repository.IngredientRepository;
import org.example.goldenheartrestaurant.modules.menu.dto.request.CreateMenuItemRequest;
import org.example.goldenheartrestaurant.modules.menu.dto.request.RecipeIngredientRequest;
import org.example.goldenheartrestaurant.modules.menu.dto.request.UpdateMenuItemRequest;
import org.example.goldenheartrestaurant.modules.menu.dto.response.MenuItemResponse;
import org.example.goldenheartrestaurant.modules.menu.dto.response.RecipeResponse;
import org.example.goldenheartrestaurant.modules.menu.entity.Category;
import org.example.goldenheartrestaurant.modules.menu.entity.MenuItem;
import org.example.goldenheartrestaurant.modules.menu.entity.MenuItemStatus;
import org.example.goldenheartrestaurant.modules.menu.entity.Recipe;
import org.example.goldenheartrestaurant.modules.menu.repository.CategoryRepository;
import org.example.goldenheartrestaurant.modules.menu.repository.MenuItemRepository;
import org.example.goldenheartrestaurant.modules.menu.repository.RecipeRepository;
import org.example.goldenheartrestaurant.modules.restaurant.entity.Branch;
import org.example.goldenheartrestaurant.modules.restaurant.repository.BranchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
/**
 * Handles menu CRUD plus recipe maintenance.
 *
 * The main tricky area is keeping recipe rows in sync when an admin edits a dish definition.
 */
public class MenuManagementService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final BranchRepository branchRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;

    @Transactional(readOnly = true)
    public PageResponse<MenuItemResponse> getMenuItems(String keyword, Integer branchId, Integer categoryId, int page, int size) {
        Page<MenuItem> menuItems = menuItemRepository.search(normalizeKeyword(keyword), branchId, categoryId, PageRequest.of(page, size));

        return PageResponse.<MenuItemResponse>builder()
                .content(menuItems.getContent().stream().map(this::toMenuItemResponse).toList())
                .page(menuItems.getNumber())
                .size(menuItems.getSize())
                .totalElements(menuItems.getTotalElements())
                .totalPages(menuItems.getTotalPages())
                .last(menuItems.isLast())
                .build()
        ;
    }

    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItemById(Integer menuItemId) {
        return toMenuItemResponse(getMenuItemOrThrow(menuItemId));
    }

    @Transactional
    public MenuItemResponse createMenuItem(CreateMenuItemRequest request) {
        validateMenuUniqueness(request.branchId(), request.categoryId(), request.name(), null);
        validateRecipeRequests(request.recipes());

        MenuItem menuItem = MenuItem.builder()
                .branch(resolveBranch(request.branchId()))
                .category(resolveCategory(request.categoryId()))
                .name(request.name().trim())
                .description(request.description())
                .price(request.price())
                .status(resolveMenuStatus(request.status()))
                .build();

        replaceRecipes(menuItem, request.recipes());

        return toMenuItemResponse(menuItemRepository.save(menuItem));
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Integer menuItemId, UpdateMenuItemRequest request) {
        MenuItem menuItem = getMenuItemOrThrow(menuItemId);
        validateMenuUniqueness(request.branchId(), request.categoryId(), request.name(), menuItemId);
        validateRecipeRequests(request.recipes());

        menuItem.setBranch(resolveBranch(request.branchId()));
        menuItem.setCategory(resolveCategory(request.categoryId()));
        menuItem.setName(request.name().trim());
        menuItem.setDescription(request.description());
        menuItem.setPrice(request.price());
        menuItem.setStatus(resolveMenuStatus(request.status()));

        // Replace-all strategy:
        // the incoming request becomes the full source of truth for the menu item's recipe.
        recipeRepository.deleteByMenuItemId(menuItemId);
        menuItem.getRecipes().clear();
        replaceRecipes(menuItem, request.recipes());

        return toMenuItemResponse(menuItemRepository.save(menuItem));
    }

    @Transactional
    public void deleteMenuItem(Integer menuItemId) {
        menuItemRepository.delete(getMenuItemOrThrow(menuItemId));
    }

    private void replaceRecipes(MenuItem menuItem, List<RecipeIngredientRequest> recipes) {
        for (RecipeIngredientRequest recipeRequest : recipes) {
            Ingredient ingredient = ingredientRepository.findById(recipeRequest.ingredientId())
                    .orElseThrow(() -> new NotFoundException("Ingredient not found: " + recipeRequest.ingredientId()));

            menuItem.getRecipes().add(
                    Recipe.builder()
                            .menuItem(menuItem)
                            .ingredient(ingredient)
                            // This quantity is later multiplied by order item quantity in kitchen stock deduction.
                            .quantity(recipeRequest.quantity())
                            .build()
            );
        }
    }

    private void validateRecipeRequests(List<RecipeIngredientRequest> recipes) {
        Set<Integer> ingredientIds = new HashSet<>();

        for (RecipeIngredientRequest recipe : recipes) {
            if (!ingredientIds.add(recipe.ingredientId())) {
                // Duplicate ingredient lines would make stock consumption ambiguous.
                throw new ConflictException("Duplicate ingredient in recipe: " + recipe.ingredientId());
            }
        }
    }

    private void validateMenuUniqueness(Integer branchId, Integer categoryId, String name, Integer menuItemId) {
        boolean exists = menuItemId == null
                ? menuItemRepository.existsByBranchIdAndCategoryIdAndNameIgnoreCase(branchId, categoryId, name)
                : menuItemRepository.existsByBranchIdAndCategoryIdAndNameIgnoreCaseAndIdNot(branchId, categoryId, name, menuItemId);

        if (exists) {
            throw new ConflictException("Menu item name already exists in this branch and category");
        }
    }

    private Branch resolveBranch(Integer branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found"));
    }

    private Category resolveCategory(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private MenuItemStatus resolveMenuStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return MenuItemStatus.AVAILABLE;
        }
        return MenuItemStatus.valueOf(status.trim().toUpperCase());
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private MenuItem getMenuItemOrThrow(Integer menuItemId) {
        return menuItemRepository.findDetailById(menuItemId)
                .orElseThrow(() -> new NotFoundException("Menu item not found"));
    }

    private MenuItemResponse toMenuItemResponse(MenuItem menuItem) {
        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getBranch().getId(),
                menuItem.getBranch().getName(),
                menuItem.getCategory().getId(),
                menuItem.getCategory().getName(),
                menuItem.getName(),
                menuItem.getDescription(),
                menuItem.getPrice(),
                menuItem.getStatus().name(),
                menuItem.getRecipes().stream()
                        .map(recipe -> new RecipeResponse(
                                recipe.getIngredient().getId(),
                                recipe.getIngredient().getName(),
                                recipe.getIngredient().getUnit(),
                                recipe.getQuantity()
                        ))
                        .toList()
        );
    }
}
