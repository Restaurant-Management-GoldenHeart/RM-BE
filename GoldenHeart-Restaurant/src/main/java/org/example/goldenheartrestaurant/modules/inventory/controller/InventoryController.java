package org.example.goldenheartrestaurant.modules.inventory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.inventory.dto.request.CreateInventoryItemRequest;
import org.example.goldenheartrestaurant.modules.inventory.dto.request.UpdateInventoryItemRequest;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventoryActionLogResponse;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventoryAlertResponse;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.InventoryItemResponse;
import org.example.goldenheartrestaurant.modules.inventory.dto.response.MeasurementUnitResponse;
import org.example.goldenheartrestaurant.modules.inventory.service.InventoryManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
/**
 * Controller quản lý inventory.
 *
 * Rule phân quyền:
 * - ADMIN, MANAGER: thêm / sửa / xóa
 * - ADMIN, MANAGER, STAFF, KITCHEN: xem
 */
public class InventoryController {

    private final InventoryManagementService inventoryManagementService;

    @GetMapping("/units")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','KITCHEN')")
    public ResponseEntity<ApiResponse<List<MeasurementUnitResponse>>> getMeasurementUnits() {
        return ResponseEntity.ok(
                ApiResponse.<List<MeasurementUnitResponse>>builder()
                        .message("Measurement units retrieved successfully")
                        .data(inventoryManagementService.getMeasurementUnits())
                        .build()
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','KITCHEN')")
    public ResponseEntity<ApiResponse<PageResponse<InventoryItemResponse>>> getInventoryItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer branchId,
            @RequestParam(defaultValue = "false") boolean lowStockOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<InventoryItemResponse>>builder()
                        .message("Inventory items retrieved successfully")
                        .data(inventoryManagementService.getInventoryItems(keyword, branchId, lowStockOnly, page, size))
                        .build()
        );
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','KITCHEN')")
    public ResponseEntity<ApiResponse<List<InventoryAlertResponse>>> getLowStockAlerts(
            @RequestParam(required = false) Integer branchId
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<InventoryAlertResponse>>builder()
                        .message("Inventory alerts retrieved successfully")
                        .data(inventoryManagementService.getLowStockAlerts(branchId))
                        .build()
        );
    }

    @GetMapping("/{inventoryId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','KITCHEN')")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> getInventoryItemById(@PathVariable Integer inventoryId) {
        return ResponseEntity.ok(
                ApiResponse.<InventoryItemResponse>builder()
                        .message("Inventory item retrieved successfully")
                        .data(inventoryManagementService.getInventoryItemById(inventoryId))
                        .build()
        );
    }

    @GetMapping("/{inventoryId}/history")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','KITCHEN')")
    public ResponseEntity<ApiResponse<PageResponse<InventoryActionLogResponse>>> getInventoryHistory(
            @PathVariable Integer inventoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<InventoryActionLogResponse>>builder()
                        .message("Inventory history retrieved successfully")
                        .data(inventoryManagementService.getInventoryHistory(inventoryId, page, size))
                        .build()
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> createInventoryItem(
            @Valid @RequestBody CreateInventoryItemRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<InventoryItemResponse>builder()
                        .message("Inventory item created successfully")
                        .data(inventoryManagementService.createInventoryItem(request, currentUser))
                        .build()
        );
    }

    @PutMapping("/{inventoryId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> updateInventoryItem(
            @PathVariable Integer inventoryId,
            @Valid @RequestBody UpdateInventoryItemRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<InventoryItemResponse>builder()
                        .message("Inventory item updated successfully")
                        .data(inventoryManagementService.updateInventoryItem(inventoryId, request, currentUser))
                        .build()
        );
    }

    @DeleteMapping("/{inventoryId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteInventoryItem(
            @PathVariable Integer inventoryId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        inventoryManagementService.deleteInventoryItem(inventoryId, currentUser);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Inventory item deleted successfully")
                        .build()
        );
    }
}
