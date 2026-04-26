package org.example.goldenheartrestaurant.modules.customer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.modules.customer.dto.request.CreateCustomerRequest;
import org.example.goldenheartrestaurant.modules.customer.dto.request.QuickCreateCustomerRequest;
import org.example.goldenheartrestaurant.modules.customer.dto.request.UpdateCustomerRequest;
import org.example.goldenheartrestaurant.modules.customer.dto.response.CustomerLookupResponse;
import org.example.goldenheartrestaurant.modules.customer.dto.response.CustomerLoyaltyTransactionResponse;
import org.example.goldenheartrestaurant.modules.customer.dto.response.CustomerResponse;
import org.example.goldenheartrestaurant.modules.customer.service.CustomerService;
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
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
/**
 * Controller customer.
 *
 * Crud CRM van giu cho ADMIN/MANAGER.
 * POS flow mo them lookup + quick-create cho STAFF.
 */
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/lookup")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF"})
    public ResponseEntity<ApiResponse<List<CustomerLookupResponse>>> lookupCustomers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<CustomerLookupResponse>>builder()
                        .message("Customers looked up successfully")
                        .data(customerService.lookupCustomers(keyword, size))
                        .build()
        );
    }

    @GetMapping
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> getCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<CustomerResponse>>builder()
                        .message("Customers retrieved successfully")
                        .data(customerService.getCustomers(keyword, page, size))
                        .build()
        );
    }

    @GetMapping("/{customerId}")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable Integer customerId) {
        return ResponseEntity.ok(
                ApiResponse.<CustomerResponse>builder()
                        .message("Customer retrieved successfully")
                        .data(customerService.getCustomerById(customerId))
                        .build()
        );
    }

    @PostMapping
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CustomerResponse>builder()
                        .message("Customer created successfully")
                        .data(customerService.createCustomer(request))
                        .build()
        );
    }

    @PostMapping("/quick-create")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF"})
    public ResponseEntity<ApiResponse<CustomerLookupResponse>> quickCreateCustomer(
            @Valid @RequestBody QuickCreateCustomerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CustomerLookupResponse>builder()
                        .message("Customer created quickly successfully")
                        .data(customerService.quickCreateCustomer(request))
                        .build()
        );
    }

    @PutMapping("/{customerId}")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Integer customerId,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<CustomerResponse>builder()
                        .message("Customer updated successfully")
                        .data(customerService.updateCustomer(customerId, request))
                        .build()
        );
    }

    @GetMapping("/{customerId}/loyalty-transactions")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF"})
    public ResponseEntity<ApiResponse<PageResponse<CustomerLoyaltyTransactionResponse>>> getCustomerLoyaltyTransactions(
            @PathVariable Integer customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<CustomerLoyaltyTransactionResponse>>builder()
                        .message("Customer loyalty transactions retrieved successfully")
                        .data(customerService.getCustomerTransactions(customerId, page, size))
                        .build()
        );
    }

    @DeleteMapping("/{customerId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Integer customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Customer deleted successfully")
                        .build()
        );
    }
}
