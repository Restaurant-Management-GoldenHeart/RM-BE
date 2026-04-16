package org.example.goldenheartrestaurant.modules.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.order.dto.request.UpdateKitchenOrderItemStatusRequest;
import org.example.goldenheartrestaurant.modules.order.dto.response.KitchenPendingOrderItemResponse;
import org.example.goldenheartrestaurant.modules.order.dto.response.OrderItemCompletionResponse;
import org.example.goldenheartrestaurant.modules.order.dto.response.OrderItemStatusChangeResponse;
import org.example.goldenheartrestaurant.modules.order.entity.OrderItemStatus;
import org.example.goldenheartrestaurant.modules.order.service.KitchenWorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1/kitchen")
@RequiredArgsConstructor
/**
 * API nghiệp vụ cho bếp.
 *
 * Endpoint hiện tại là complete order item, và thao tác này không chỉ đổi status món
 * mà còn đụng tới tồn kho, nên chỉ ADMIN và KITCHEN mới được phép gọi.
 */
public class KitchenController {

    private final KitchenWorkflowService kitchenWorkflowService;

    @GetMapping("/orders/pending")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<List<KitchenPendingOrderItemResponse>>> getPendingKitchenItems(
            @RequestParam(required = false) Integer branchId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<KitchenPendingOrderItemResponse>>builder()
                        .message("Kitchen items retrieved successfully")
                        .data(kitchenWorkflowService.getPendingItems(branchId, currentUser))
                        .build()
        );
    }

    @PutMapping("/order-items/{orderItemId}/status")
    @Secured({"ROLE_ADMIN", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<OrderItemStatusChangeResponse>> updateOrderItemStatus(
            @PathVariable Integer orderItemId,
            @Valid @RequestBody UpdateKitchenOrderItemStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        OrderItemStatus targetStatus;
        try {
            targetStatus = OrderItemStatus.valueOf(request.status().trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new org.example.goldenheartrestaurant.common.exception.ConflictException("Unsupported kitchen status");
        }

        return ResponseEntity.ok(
                ApiResponse.<OrderItemStatusChangeResponse>builder()
                        .message("Order item status updated successfully")
                        .data(kitchenWorkflowService.changeOrderItemStatus(orderItemId, targetStatus, request.reason(), currentUser))
                        .build()
        );
    }

    @PostMapping("/order-items/{orderItemId}/complete")
    @Secured({"ROLE_ADMIN", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<OrderItemCompletionResponse>> completeOrderItem(
            @PathVariable Integer orderItemId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<OrderItemCompletionResponse>builder()
                        .message("Order item completed successfully")
                        .data(kitchenWorkflowService.completeOrderItem(orderItemId, currentUser))
                        .build()
        );
    }
}
