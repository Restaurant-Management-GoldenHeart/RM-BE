package org.example.goldenheartrestaurant.modules.menu.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.modules.menu.dto.request.CreateMenuItemRequest;
import org.example.goldenheartrestaurant.modules.menu.dto.request.UpdateMenuItemRequest;
import org.example.goldenheartrestaurant.modules.menu.dto.response.MenuItemResponse;
import org.example.goldenheartrestaurant.modules.menu.service.MenuManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/menu-items")
@RequiredArgsConstructor
/**
 * Controller quản lý menu.
 *
 * Rule phân quyền ở mức controller:
 * - ADMIN: create/update/delete
 * - ADMIN, MANAGER, STAFF, KITCHEN: read
 *
 * Lý do read mở rộng:
 * - staff cần xem catalog món để phục vụ order
 * - kitchen cần xem cấu hình món/recipe để vận hành
 */
public class MenuItemController {

    private final MenuManagementService menuManagementService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','KITCHEN')")
    public ResponseEntity<ApiResponse<PageResponse<MenuItemResponse>>> getMenuItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<MenuItemResponse>>builder()
                        .message("Menu items retrieved successfully")
                        .data(menuManagementService.getMenuItems(keyword, branchId, categoryId, page, size))
                        .build()
        );
    }

    @GetMapping("/{menuItemId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','KITCHEN')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getMenuItemById(@PathVariable Integer menuItemId) {
        return ResponseEntity.ok(
                ApiResponse.<MenuItemResponse>builder()
                        .message("Menu item retrieved successfully")
                        .data(menuManagementService.getMenuItemById(menuItemId))
                        .build()
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> createMenuItem(@Valid @RequestBody CreateMenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<MenuItemResponse>builder()
                        .message("Menu item created successfully")
                        .data(menuManagementService.createMenuItem(request))
                        .build()
        );
    }

    @PutMapping("/{menuItemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateMenuItem(
            @PathVariable Integer menuItemId,
            @Valid @RequestBody UpdateMenuItemRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<MenuItemResponse>builder()
                        .message("Menu item updated successfully")
                        .data(menuManagementService.updateMenuItem(menuItemId, request))
                        .build()
        );
    }

    @DeleteMapping("/{menuItemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(@PathVariable Integer menuItemId) {
        menuManagementService.deleteMenuItem(menuItemId);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Menu item deleted successfully")
                        .build()
        );
    }
}
