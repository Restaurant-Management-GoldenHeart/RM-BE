package org.example.goldenheartrestaurant.modules.menu.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.modules.inventory.entity.Ingredient;
import org.example.goldenheartrestaurant.modules.inventory.entity.Inventory;
import org.example.goldenheartrestaurant.modules.inventory.repository.IngredientRepository;
import org.example.goldenheartrestaurant.modules.inventory.repository.InventoryRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final InventoryRepository inventoryRepository;
    private final RecipeRepository recipeRepository;
    private final MenuItemImageStorageService menuItemImageStorageService;

    @Transactional(readOnly = true)
    public PageResponse<MenuItemResponse> getMenuItems(String keyword, Integer branchId, Integer categoryId, int page, int size) {
        Page<MenuItem> menuItems = menuItemRepository.search(normalizeKeyword(keyword), branchId, categoryId, PageRequest.of(page, size));
        Map<Integer, MenuAvailabilitySnapshot> availabilityByMenuItemId = buildAvailabilityMap(menuItems.getContent());

        return PageResponse.<MenuItemResponse>builder()
                .content(menuItems.getContent().stream()
                        .map(menuItem -> toMenuItemResponse(menuItem, availabilityByMenuItemId.get(menuItem.getId())))
                        .toList())
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
        MenuItem menuItem = getMenuItemOrThrow(menuItemId);
        return toMenuItemResponse(menuItem, evaluateAvailability(menuItem));
    }

    @Transactional
    public MenuItemResponse createMenuItem(CreateMenuItemRequest request, MultipartFile imageFile) {
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

        MenuItemImageStorageService.StoredMenuItemImage storedImage = menuItemImageStorageService.uploadMenuItemImage(
                imageFile,
                request.branchId(),
                request.name()
        );
        if (storedImage != null) {
            menuItem.setImageUrl(storedImage.imageUrl());
            menuItem.setImagePublicId(storedImage.imagePublicId());
        }

        try {
            MenuItem savedMenuItem = menuItemRepository.save(menuItem);
            return toMenuItemResponse(savedMenuItem, evaluateAvailability(savedMenuItem));
        } catch (RuntimeException exception) {
            if (storedImage != null) {
                menuItemImageStorageService.deleteMenuItemImage(storedImage.imagePublicId());
            }
            throw exception;
        }
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Integer menuItemId, UpdateMenuItemRequest request, MultipartFile imageFile) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new NotFoundException("Menu item not found"));
        Integer nextBranchId = request.branchId() != null ? request.branchId() : menuItem.getBranch().getId();
        Integer nextCategoryId = request.categoryId() != null ? request.categoryId() : menuItem.getCategory().getId();
        String nextName = mergeRequiredText(request.name(), menuItem.getName(), "Menu item name must not be blank");
        BigDecimal nextPrice = request.price() != null ? request.price() : menuItem.getPrice();
        String nextDescription = request.description() != null ? request.description() : menuItem.getDescription();
        MenuItemStatus nextStatus = StringUtils.hasText(request.status())
                ? resolveMenuStatus(request.status())
                : menuItem.getStatus();
        List<RecipeIngredientRequest> nextRecipes = request.recipes();

        if (nextPrice == null) {
            throw new ConflictException("Menu item price must not be null");
        }
        if (nextPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException("Menu item price must not be negative");
        }
        if (nextRecipes != null) {
            if (nextRecipes.isEmpty()) {
                throw new ConflictException("Recipe list must not be empty when updating menu item");
            }
            validateRecipeRequests(nextRecipes);
        }

        validateMenuUniqueness(nextBranchId, nextCategoryId, nextName, menuItemId);

        menuItem.setBranch(resolveBranch(nextBranchId));
        menuItem.setCategory(resolveCategory(nextCategoryId));
        menuItem.setName(nextName);
        menuItem.setDescription(nextDescription);
        menuItem.setPrice(nextPrice);
        menuItem.setStatus(nextStatus);

        String previousImagePublicId = menuItem.getImagePublicId();
        MenuItemImageStorageService.StoredMenuItemImage storedImage = menuItemImageStorageService.uploadMenuItemImage(
                imageFile,
                nextBranchId,
                nextName
        );
        if (storedImage != null) {
            menuItem.setImageUrl(storedImage.imageUrl());
            menuItem.setImagePublicId(storedImage.imagePublicId());
        }

        // Save trước để các field chính của menu item được cập nhật ổn định.
        MenuItem savedMenuItem;
        try {
            savedMenuItem = menuItemRepository.saveAndFlush(menuItem);
        } catch (RuntimeException exception) {
            if (storedImage != null) {
                menuItemImageStorageService.deleteMenuItemImage(storedImage.imagePublicId());
            }
            throw exception;
        }

        // Xóa toàn bộ recipe cũ rồi insert recipe mới.
        // Cách này đơn giản và an toàn hơn so với việc diff từng dòng recipe.
        if (nextRecipes != null) {
            recipeRepository.deleteByMenuItemId(menuItemId);
            recipeRepository.saveAll(buildRecipes(savedMenuItem, nextRecipes));
        }

        if (storedImage != null && StringUtils.hasText(previousImagePublicId)
                && !previousImagePublicId.equals(storedImage.imagePublicId())) {
            menuItemImageStorageService.deleteMenuItemImage(previousImagePublicId);
        }

        MenuItem refreshedMenuItem = getMenuItemOrThrow(menuItemId);
        return toMenuItemResponse(refreshedMenuItem, evaluateAvailability(refreshedMenuItem));
    }

    @Transactional
    public void deleteMenuItem(Integer menuItemId) {
        menuItemRepository.delete(getMenuItemOrThrow(menuItemId));
    }

    private void replaceRecipes(MenuItem menuItem, List<RecipeIngredientRequest> recipes) {
        menuItem.getRecipes().addAll(buildRecipes(menuItem, recipes));
    }

    private void validateRecipeRequests(List<RecipeIngredientRequest> recipes) {
        if (recipes == null) {
            return;
        }
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

    private String mergeRequiredText(String candidate, String currentValue, String message) {
        if (candidate == null) {
            return currentValue;
        }
        if (!StringUtils.hasText(candidate)) {
            throw new ConflictException(message);
        }
        return candidate.trim();
    }

    private List<Recipe> buildRecipes(MenuItem menuItem, List<RecipeIngredientRequest> recipes) {
        return recipes.stream()
                .map(recipeRequest -> {
                    Ingredient ingredient = ingredientRepository
                            .findByIdAndBranchId(recipeRequest.ingredientId(), menuItem.getBranch().getId())
                            .orElseThrow(() -> new NotFoundException(
                                    "Ingredient not found in branch: " + recipeRequest.ingredientId()
                            ));

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

    private MenuItemResponse toMenuItemResponse(MenuItem menuItem, MenuAvailabilitySnapshot availability) {
        MenuAvailabilitySnapshot resolvedAvailability = availability != null
                ? availability
                : evaluateAvailability(menuItem);

        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getBranch().getId(),
                menuItem.getBranch().getName(),
                menuItem.getCategory().getId(),
                menuItem.getCategory().getName(),
                menuItem.getName(),
                menuItem.getDescription(),
                menuItem.getPrice(),
                menuItem.getImageUrl(),
                menuItem.getSoldCount() != null ? menuItem.getSoldCount() : 0L,
                menuItem.getStatus().name(),
                resolvedAvailability.status().name(),
                resolvedAvailability.available(),
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

    private Map<Integer, MenuAvailabilitySnapshot> buildAvailabilityMap(Collection<MenuItem> menuItems) {
        Map<Integer, MenuAvailabilitySnapshot> result = new HashMap<>();
        if (menuItems == null || menuItems.isEmpty()) {
            return result;
        }

        Map<Integer, List<MenuItem>> menuItemsByBranch = new HashMap<>();
        for (MenuItem menuItem : menuItems) {
            menuItemsByBranch.computeIfAbsent(menuItem.getBranch().getId(), ignored -> new java.util.ArrayList<>())
                    .add(menuItem);
        }

        for (Map.Entry<Integer, List<MenuItem>> entry : menuItemsByBranch.entrySet()) {
            Integer branchId = entry.getKey();
            List<MenuItem> branchMenuItems = entry.getValue();
            List<Integer> ingredientIds = branchMenuItems.stream()
                    .flatMap(menuItem -> menuItem.getRecipes().stream())
                    .map(recipe -> recipe.getIngredient().getId())
                    .distinct()
                    .toList();

            Map<Integer, Inventory> inventoryByIngredientId = ingredientIds.isEmpty()
                    ? Map.of()
                    : inventoryRepository.findAllByBranchIdAndIngredientIds(branchId, ingredientIds)
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(
                            inventory -> inventory.getIngredient().getId(),
                            java.util.function.Function.identity()
                    ));

            for (MenuItem menuItem : branchMenuItems) {
                result.put(menuItem.getId(), evaluateAvailability(menuItem, inventoryByIngredientId));
            }
        }

        return result;
    }

    private MenuAvailabilitySnapshot evaluateAvailability(MenuItem menuItem) {
        List<Integer> ingredientIds = menuItem.getRecipes().stream()
                .map(recipe -> recipe.getIngredient().getId())
                .distinct()
                .toList();

        Map<Integer, Inventory> inventoryByIngredientId = ingredientIds.isEmpty()
                ? Map.of()
                : inventoryRepository.findAllByBranchIdAndIngredientIds(menuItem.getBranch().getId(), ingredientIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        inventory -> inventory.getIngredient().getId(),
                        java.util.function.Function.identity()
                ));

        return evaluateAvailability(menuItem, inventoryByIngredientId);
    }

    private MenuAvailabilitySnapshot evaluateAvailability(MenuItem menuItem,
                                                          Map<Integer, Inventory> inventoryByIngredientId) {
        if (menuItem.getStatus() != MenuItemStatus.AVAILABLE) {
            return new MenuAvailabilitySnapshot(MenuItemStatus.OUT_OF_STOCK, false);
        }

        if (menuItem.getRecipes() == null || menuItem.getRecipes().isEmpty()) {
            return new MenuAvailabilitySnapshot(MenuItemStatus.OUT_OF_STOCK, false);
        }

        for (Recipe recipe : menuItem.getRecipes()) {
            Inventory inventory = inventoryByIngredientId.get(recipe.getIngredient().getId());
            if (inventory == null) {
                return new MenuAvailabilitySnapshot(MenuItemStatus.OUT_OF_STOCK, false);
            }

            BigDecimal currentQuantity = inventory.getQuantity() != null ? inventory.getQuantity() : BigDecimal.ZERO;
            BigDecimal requiredQuantity = recipe.getQuantity() != null ? recipe.getQuantity() : BigDecimal.ZERO;
            if (currentQuantity.compareTo(requiredQuantity) < 0) {
                return new MenuAvailabilitySnapshot(MenuItemStatus.OUT_OF_STOCK, false);
            }
        }

        return new MenuAvailabilitySnapshot(MenuItemStatus.AVAILABLE, true);
    }

    private record MenuAvailabilitySnapshot(MenuItemStatus status, boolean available) {
    }
}
