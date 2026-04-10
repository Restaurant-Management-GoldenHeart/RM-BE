package org.example.goldenheartrestaurant.modules.order.controller;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.order.dto.response.OrderItemCompletionResponse;
import org.example.goldenheartrestaurant.modules.order.service.KitchenProductionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kitchen")
@RequiredArgsConstructor
/**
 * Kitchen operations API.
 *
 * Only ADMIN and KITCHEN can complete order items because this action also mutates inventory.
 */
public class KitchenController {

    private final KitchenProductionService kitchenProductionService;

    @PostMapping("/order-items/{orderItemId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','KITCHEN')")
    public ResponseEntity<ApiResponse<OrderItemCompletionResponse>> completeOrderItem(
            @PathVariable Integer orderItemId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<OrderItemCompletionResponse>builder()
                        .message("Order item completed and stock deducted successfully")
                        .data(kitchenProductionService.completeOrderItem(orderItemId, currentUser))
                        .build()
        );
    }
}
