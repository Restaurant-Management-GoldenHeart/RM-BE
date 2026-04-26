package org.example.goldenheartrestaurant.modules.customer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.modules.customer.dto.request.CreateCustomerTierRequest;
import org.example.goldenheartrestaurant.modules.customer.dto.request.UpdateCustomerTierRequest;
import org.example.goldenheartrestaurant.modules.customer.dto.response.CustomerTierResponse;
import org.example.goldenheartrestaurant.modules.customer.service.CustomerTierService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer-tiers")
@RequiredArgsConstructor
public class CustomerTierController {

    private final CustomerTierService customerTierService;

    @GetMapping
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<List<CustomerTierResponse>>> getCustomerTiers(
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<CustomerTierResponse>>builder()
                        .message("Customer tiers retrieved successfully")
                        .data(customerTierService.getCustomerTiers(activeOnly))
                        .build()
        );
    }

    @GetMapping("/{tierId}")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<CustomerTierResponse>> getCustomerTierById(@PathVariable Integer tierId) {
        return ResponseEntity.ok(
                ApiResponse.<CustomerTierResponse>builder()
                        .message("Customer tier retrieved successfully")
                        .data(customerTierService.getCustomerTierById(tierId))
                        .build()
        );
    }

    @PostMapping
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<CustomerTierResponse>> createCustomerTier(
            @Valid @RequestBody CreateCustomerTierRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CustomerTierResponse>builder()
                        .message("Customer tier created successfully")
                        .data(customerTierService.createCustomerTier(request))
                        .build()
        );
    }

    @PutMapping("/{tierId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<CustomerTierResponse>> updateCustomerTier(
            @PathVariable Integer tierId,
            @Valid @RequestBody UpdateCustomerTierRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<CustomerTierResponse>builder()
                        .message("Customer tier updated successfully")
                        .data(customerTierService.updateCustomerTier(tierId, request))
                        .build()
        );
    }

    @DeleteMapping("/{tierId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> deactivateCustomerTier(@PathVariable Integer tierId) {
        customerTierService.deactivateCustomerTier(tierId);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Customer tier deactivated successfully")
                        .build()
        );
    }
}
