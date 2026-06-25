package org.example.goldenheartrestaurant.modules.menu.service;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.example.goldenheartrestaurant.common.exception.NotFoundException;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.modules.menu.dto.request.CreateCategoryRequest;
import org.example.goldenheartrestaurant.modules.menu.dto.request.UpdateCategoryRequest;
import org.example.goldenheartrestaurant.modules.menu.dto.response.CategoryResponse;
import org.example.goldenheartrestaurant.modules.menu.entity.Category;
import org.example.goldenheartrestaurant.modules.menu.entity.ProductionStation;
import org.example.goldenheartrestaurant.modules.menu.repository.CategoryRepository;
import org.example.goldenheartrestaurant.modules.menu.repository.MenuItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
/**
 * Service CRUD cho danh muc mon an.
 *
 * Rule nghiep vu:
 * - Ten danh muc phai unique toan he thong de FE/BE khong bi lap y nghia.
 * - Khong cho xoa danh muc neu van con menu item dang tham chieu.
 */
public class CategoryManagementService {

    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getCategories(String keyword, int page, int size) {
        Page<Category> categories = categoryRepository.search(normalizeKeyword(keyword), PageRequest.of(page, size));

        return PageResponse.<CategoryResponse>builder()
                .content(categories.getContent().stream().map(this::toCategoryResponse).toList())
                .page(categories.getNumber())
                .size(categories.getSize())
                .totalElements(categories.getTotalElements())
                .totalPages(categories.getTotalPages())
                .last(categories.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Integer categoryId) {
        return toCategoryResponse(getCategoryOrThrow(categoryId));
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        validateCategoryNameUniqueness(request.name(), null);

        Category category = Category.builder()
                .name(request.name().trim())
                .description(normalizeDescription(request.description()))
                .productionStation(resolveProductionStation(request.productionStation()))
                .build();

        return toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Integer categoryId, UpdateCategoryRequest request) {
        Category category = getCategoryOrThrow(categoryId);
        validateCategoryNameUniqueness(request.name(), categoryId);

        category.setName(request.name().trim());
        category.setDescription(normalizeDescription(request.description()));
        category.setProductionStation(resolveProductionStation(request.productionStation()));

        return toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Integer categoryId) {
        Category category = getCategoryOrThrow(categoryId);

        if (menuItemRepository.existsByCategoryId(categoryId)) {
            throw new ConflictException("Category is being used by menu items and cannot be deleted");
        }

        categoryRepository.delete(category);
    }

    private void validateCategoryNameUniqueness(String name, Integer categoryId) {
        String normalizedName = name.trim();

        boolean exists = categoryId == null
                ? categoryRepository.existsByNameIgnoreCase(normalizedName)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, categoryId);

        if (exists) {
            throw new ConflictException("Category name already exists");
        }
    }

    private Category getCategoryOrThrow(Integer categoryId) {
        return categoryRepository.findDetailById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private String normalizeDescription(String description) {
        return StringUtils.hasText(description) ? description.trim() : null;
    }

    private ProductionStation resolveProductionStation(String productionStation) {
        if (!StringUtils.hasText(productionStation)) {
            return ProductionStation.KITCHEN;
        }
        try {
            return ProductionStation.valueOf(productionStation.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Unsupported production station");
        }
    }

    private ProductionStation resolveCategoryStation(Category category) {
        return category.getProductionStation() != null
                ? category.getProductionStation()
                : ProductionStation.KITCHEN;
    }

    private CategoryResponse toCategoryResponse(Category category) {
        int menuItemCount = category.getMenuItems() != null ? category.getMenuItems().size() : 0;

        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                resolveCategoryStation(category).name(),
                menuItemCount
        );
    }
}
