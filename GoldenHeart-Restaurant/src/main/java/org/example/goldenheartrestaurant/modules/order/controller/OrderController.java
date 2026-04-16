package org.example.goldenheartrestaurant.modules.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.order.dto.request.CreateOrderRequest;
import org.example.goldenheartrestaurant.modules.order.dto.response.OrderItemStatusChangeResponse;
import org.example.goldenheartrestaurant.modules.order.dto.response.OrderResponse;
import org.example.goldenheartrestaurant.modules.order.service.OrderManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderManagementService orderManagementService;

    @PostMapping
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF"})
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<OrderResponse>builder()
                        .message("Order created successfully")
                        .data(orderManagementService.createOrder(request, currentUser))
                        .build()
        );
    }

    @GetMapping("/{orderId}")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .message("Order retrieved successfully")
                        .data(orderManagementService.getOrderById(orderId, currentUser))
                        .build()
        );
    }

    @PutMapping("/order-items/{orderItemId}/serve")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF"})
    public ResponseEntity<ApiResponse<OrderItemStatusChangeResponse>> serveOrderItem(
            @PathVariable Integer orderItemId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<OrderItemStatusChangeResponse>builder()
                        .message("Order item marked as served successfully")
                        .data(orderManagementService.serveOrderItem(orderItemId, currentUser))
                        .build()
        );
    }
}
