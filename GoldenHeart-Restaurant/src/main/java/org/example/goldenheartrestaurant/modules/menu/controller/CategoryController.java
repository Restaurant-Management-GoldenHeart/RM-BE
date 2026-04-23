package org.example.goldenheartrestaurant.modules.menu.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.modules.menu.dto.request.CreateCategoryRequest;
import org.example.goldenheartrestaurant.modules.menu.dto.request.UpdateCategoryRequest;
import org.example.goldenheartrestaurant.modules.menu.dto.response.CategoryResponse;
import org.example.goldenheartrestaurant.modules.menu.service.CategoryManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
/**
 * CRUD API cho danh muc mon an.
 *
 * Rule phan quyen:
 * - ADMIN: create/update/delete
 * - ADMIN, MANAGER, STAFF, KITCHEN: read
 */
public class CategoryController {

    private final CategoryManagementService categoryManagementService;

    @GetMapping
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<CategoryResponse>>builder()
                        .message("Categories retrieved successfully")
                        .data(categoryManagementService.getCategories(keyword, page, size))
                        .build()
        );
    }

    @GetMapping("/{categoryId}")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(
                ApiResponse.<CategoryResponse>builder()
                        .message("Category retrieved successfully")
                        .data(categoryManagementService.getCategoryById(categoryId))
                        .build()
        );
    }

    @PostMapping
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CategoryResponse>builder()
                        .message("Category created successfully")
                        .data(categoryManagementService.createCategory(request))
                        .build()
        );
    }

    @PutMapping("/{categoryId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Integer categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<CategoryResponse>builder()
                        .message("Category updated successfully")
                        .data(categoryManagementService.updateCategory(categoryId, request))
                        .build()
        );
    }

    @DeleteMapping("/{categoryId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Integer categoryId) {
        categoryManagementService.deleteCategory(categoryId);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Category deleted successfully")
                        .build()
        );
    }
}
