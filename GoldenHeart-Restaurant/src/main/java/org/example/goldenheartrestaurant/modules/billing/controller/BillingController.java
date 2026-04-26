package org.example.goldenheartrestaurant.modules.billing.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.billing.dto.request.CreateBillRequest;
import org.example.goldenheartrestaurant.modules.billing.dto.request.CreatePaymentRequest;
import org.example.goldenheartrestaurant.modules.billing.dto.response.BillResponse;
import org.example.goldenheartrestaurant.modules.billing.dto.response.CheckoutPreviewResponse;
import org.example.goldenheartrestaurant.modules.billing.service.BillingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/preview")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF"})
    public ResponseEntity<ApiResponse<CheckoutPreviewResponse>> previewCheckout(
            @RequestParam Integer orderId,
            @RequestParam(required = false) BigDecimal discount,
            @RequestParam(required = false) BigDecimal taxRate,
            @RequestParam(defaultValue = "false") boolean applyLoyaltyDiscount,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<CheckoutPreviewResponse>builder()
                        .message("Checkout preview retrieved successfully")
                        .data(billingService.previewCheckout(orderId, discount, taxRate, applyLoyaltyDiscount, currentUser))
                        .build()
        );
    }

    @PostMapping
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF"})
    public ResponseEntity<ApiResponse<BillResponse>> createBill(
            @Valid @RequestBody CreateBillRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<BillResponse>builder()
                        .message("Bill created successfully")
                        .data(billingService.createBill(request, currentUser))
                        .build()
        );
    }

    @PostMapping("/{billId}/payments")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF"})
    public ResponseEntity<ApiResponse<BillResponse>> addPayment(
            @PathVariable Integer billId,
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<BillResponse>builder()
                        .message("Payment recorded successfully")
                        .data(billingService.addPayment(billId, request, currentUser))
                        .build()
        );
    }
}
