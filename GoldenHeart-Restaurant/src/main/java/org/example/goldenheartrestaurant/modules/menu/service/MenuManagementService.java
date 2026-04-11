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
 * Service xử lý CRUD menu và đồng bộ recipe.
 *
 * Điểm khó nhất ở đây là:
 * - kiểm tra uniqueness của tên món theo branch + category
 * - validate recipe không bị trùng ingredient
 * - thay recipe cũ bằng recipe mới một cách nhất quán khi update món
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
        // Trước khi tạo món phải kiểm tra:
        // - tên món có bị trùng trong cùng branch/category hay không
        // - recipe có bị lặp ingredient hay không
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
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new NotFoundException("Menu item not found"));
        validateMenuUniqueness(request.branchId(), request.categoryId(), request.name(), menuItemId);
        validateRecipeRequests(request.recipes());

        menuItem.setBranch(resolveBranch(request.branchId()));
        menuItem.setCategory(resolveCategory(request.categoryId()));
        menuItem.setName(request.name().trim());
        menuItem.setDescription(request.description());
        menuItem.setPrice(request.price());
        menuItem.setStatus(resolveMenuStatus(request.status()));

        // Save trước để các field chính của menu item được cập nhật ổn định.
        MenuItem savedMenuItem = menuItemRepository.saveAndFlush(menuItem);

        // Xóa toàn bộ recipe cũ rồi insert recipe mới.
        // Cách này đơn giản và an toàn hơn so với việc diff từng dòng recipe.
        recipeRepository.deleteByMenuItemId(menuItemId);
        recipeRepository.saveAll(buildRecipes(savedMenuItem, request.recipes()));

        return toMenuItemResponse(getMenuItemOrThrow(menuItemId));
    }

    @Transactional
    public void deleteMenuItem(Integer menuItemId) {
        menuItemRepository.delete(getMenuItemOrThrow(menuItemId));
    }

    private void replaceRecipes(MenuItem menuItem, List<RecipeIngredientRequest> recipes) {
        menuItem.getRecipes().addAll(buildRecipes(menuItem, recipes));
    }

    private void validateRecipeRequests(List<RecipeIngredientRequest> recipes) {
        Set<Integer> ingredientIds = new HashSet<>();

        for (RecipeIngredientRequest recipe : recipes) {
            if (!ingredientIds.add(recipe.ingredientId())) {
                // Nếu cùng một ingredient xuất hiện 2 lần trong recipe,
                // nghiệp vụ trừ kho về sau sẽ mơ hồ và dễ sai.
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
            // Không truyền status thì mặc định món đang bán được.
            return MenuItemStatus.AVAILABLE;
        }
        return MenuItemStatus.valueOf(status.trim().toUpperCase());
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private List<Recipe> buildRecipes(MenuItem menuItem, List<RecipeIngredientRequest> recipes) {
        return recipes.stream()
                .map(recipeRequest -> {
                    Ingredient ingredient = ingredientRepository.findById(recipeRequest.ingredientId())
                            .orElseThrow(() -> new NotFoundException("Ingredient not found: " + recipeRequest.ingredientId()));

                    return Recipe.builder()
                            .menuItem(menuItem)
                            .ingredient(ingredient)
                            .quantity(recipeRequest.quantity())
                            .build();
                })
                .toList();
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
                                recipe.getIngredient().resolveUnitSymbol(),
                                recipe.getQuantity()
                        ))
                        .toList()
        );
    }
}
