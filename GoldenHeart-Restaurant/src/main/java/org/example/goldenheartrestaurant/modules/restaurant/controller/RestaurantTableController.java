package org.example.goldenheartrestaurant.modules.restaurant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.order.dto.response.OrderResponse;
import org.example.goldenheartrestaurant.modules.order.service.OrderManagementService;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.CreateRestaurantTableRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.MergeTablesRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.SplitTableRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.UpdateTableStatusRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.UpdateRestaurantTableRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.response.RestaurantTableResponse;
import org.example.goldenheartrestaurant.modules.restaurant.dto.response.TableOrderTransferResponse;
import org.example.goldenheartrestaurant.modules.restaurant.service.RestaurantTableService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
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
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
public class RestaurantTableController {

    private final RestaurantTableService restaurantTableService;
    private final OrderManagementService orderManagementService;

    @PostMapping
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<RestaurantTableResponse>> createTable(
            @Valid @RequestBody CreateRestaurantTableRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<RestaurantTableResponse>builder()
                        .message("Table created successfully")
                        .data(restaurantTableService.createTable(request, currentUser))
                        .build()
        );
    }

    @GetMapping
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<List<RestaurantTableResponse>>> getTables(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "q") String keyword,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<RestaurantTableResponse>>builder()
                        .message("Tables retrieved successfully")
                        .data(restaurantTableService.getTables(branchId, status, keyword, currentUser))
                        .build()
        );
    }

    @GetMapping("/{tableId}")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<RestaurantTableResponse>> getTableById(
            @PathVariable Integer tableId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<RestaurantTableResponse>builder()
                        .message("Table retrieved successfully")
                        .data(restaurantTableService.getTableById(tableId, currentUser))
                        .build()
        );
    }

    @PutMapping("/{tableId}")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<RestaurantTableResponse>> updateTable(
            @PathVariable Integer tableId,
            @Valid @RequestBody UpdateRestaurantTableRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<RestaurantTableResponse>builder()
                        .message("Table updated successfully")
                        .data(restaurantTableService.updateTable(tableId, request, currentUser))
                        .build()
        );
    }

    @DeleteMapping("/{tableId}")
    @Secured({"ROLE_ADMIN"})
    public ResponseEntity<ApiResponse<Void>> deleteTable(
            @PathVariable Integer tableId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        restaurantTableService.deleteTable(tableId, currentUser);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Table deleted successfully")
                        .build()
        );
    }

    @PutMapping("/{tableId}/status")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF"})
    public ResponseEntity<ApiResponse<RestaurantTableResponse>> updateTableStatus(
            @PathVariable Integer tableId,
            @Valid @RequestBody UpdateTableStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<RestaurantTableResponse>builder()
                        .message("Table status updated successfully")
                        .data(restaurantTableService.updateTableStatus(tableId, request, currentUser))
                        .build()
        );
    }

    @GetMapping("/{tableId}/active-order")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<OrderResponse>> getActiveOrderByTable(
            @PathVariable Integer tableId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        OrderResponse activeOrder = orderManagementService.getActiveOrderByTableId(tableId, currentUser);

        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .message(activeOrder != null ? "Active order retrieved successfully" : "No active order for this table")
                        .data(activeOrder)
                        .build()
        );
    }

    @PostMapping("/{tableId}/split")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF"})
    public ResponseEntity<ApiResponse<TableOrderTransferResponse>> splitTable(
            @PathVariable Integer tableId,
            @Valid @RequestBody SplitTableRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<TableOrderTransferResponse>builder()
                        .message("Table split successfully")
                        .data(orderManagementService.splitTable(tableId, request, currentUser))
                        .build()
        );
    }

    @PostMapping("/merge")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF"})
    public ResponseEntity<ApiResponse<TableOrderTransferResponse>> mergeTables(
            @Valid @RequestBody MergeTablesRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<TableOrderTransferResponse>builder()
                        .message("Tables merged successfully")
                        .data(orderManagementService.mergeTables(request, currentUser))
                        .build()
        );
    }
}
