package org.example.goldenheartrestaurant.modules.restaurant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.CreateBranchRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.UpdateBranchRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.response.BranchResponse;
import org.example.goldenheartrestaurant.modules.restaurant.service.BranchService;
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
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getBranches(
            @RequestParam(required = false) Integer restaurantId,
            @RequestParam(required = false, name = "q") String keyword
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<BranchResponse>>builder()
                        .message("Branches retrieved successfully")
                        .data(branchService.getBranches(restaurantId, keyword))
                        .build()
        );
    }

    @GetMapping("/{branchId}")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<BranchResponse>> getBranchById(@PathVariable Integer branchId) {
        return ResponseEntity.ok(
                ApiResponse.<BranchResponse>builder()
                        .message("Branch retrieved successfully")
                        .data(branchService.getBranchById(branchId))
                        .build()
        );
    }

    @PostMapping
    @Secured({"ROLE_ADMIN"})
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(
            @Valid @RequestBody CreateBranchRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<BranchResponse>builder()
                        .message("Branch created successfully")
                        .data(branchService.createBranch(request, currentUser))
                        .build()
        );
    }

    @PutMapping("/{branchId}")
    @Secured({"ROLE_ADMIN"})
    public ResponseEntity<ApiResponse<BranchResponse>> updateBranch(
            @PathVariable Integer branchId,
            @Valid @RequestBody UpdateBranchRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<BranchResponse>builder()
                        .message("Branch updated successfully")
                        .data(branchService.updateBranch(branchId, request, currentUser))
                        .build()
        );
    }

    @DeleteMapping("/{branchId}")
    @Secured({"ROLE_ADMIN"})
    public ResponseEntity<ApiResponse<Void>> deleteBranch(
            @PathVariable Integer branchId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        branchService.deleteBranch(branchId, currentUser);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Branch deleted successfully")
                        .build()
        );
    }
}
