package org.example.goldenheartrestaurant.modules.paymentgateway.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.payos.PayOsWebhookRequest;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.response.PayOsWebhookAckResponse;
import org.example.goldenheartrestaurant.modules.paymentgateway.dto.response.PaymentGatewayTransactionResponse;
import org.example.goldenheartrestaurant.modules.paymentgateway.service.PaymentGatewayService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payment-gateways")
@RequiredArgsConstructor
public class PaymentGatewayController {

    private final PaymentGatewayService paymentGatewayService;

    @GetMapping("/transactions/{transactionId}")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF"})
    public ResponseEntity<ApiResponse<PaymentGatewayTransactionResponse>> getTransactionById(
            @PathVariable Integer transactionId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<PaymentGatewayTransactionResponse>builder()
                        .message("Payment gateway transaction retrieved successfully")
                        .data(paymentGatewayService.getTransactionById(transactionId, currentUser))
                        .build()
        );
    }

    @PostMapping("/payos/webhook")
    public ResponseEntity<ApiResponse<PayOsWebhookAckResponse>> handlePayOsWebhook(
            @Valid @RequestBody PayOsWebhookRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<PayOsWebhookAckResponse>builder()
                        .message("payOS webhook processed")
                        .data(paymentGatewayService.processPayOsWebhook(request))
                        .build()
        );
    }
}
