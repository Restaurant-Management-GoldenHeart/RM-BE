package org.example.goldenheartrestaurant.modules.restaurant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.CreateDiningAreaRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.request.UpdateDiningAreaRequest;
import org.example.goldenheartrestaurant.modules.restaurant.dto.response.DiningAreaResponse;
import org.example.goldenheartrestaurant.modules.restaurant.service.DiningAreaService;
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
@RequestMapping("/api/v1/dining-areas")
@RequiredArgsConstructor
public class DiningAreaController {

    private final DiningAreaService diningAreaService;

    @GetMapping
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<List<DiningAreaResponse>>> getDiningAreas(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false, name = "q") String keyword,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<DiningAreaResponse>>builder()
                        .message("Dining areas retrieved successfully")
                        .data(diningAreaService.getDiningAreas(branchId, active, keyword, currentUser))
                        .build()
        );
    }

    @GetMapping("/{areaId}")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<DiningAreaResponse>> getDiningAreaById(
            @PathVariable Integer areaId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<DiningAreaResponse>builder()
                        .message("Dining area retrieved successfully")
                        .data(diningAreaService.getDiningAreaById(areaId, currentUser))
                        .build()
        );
    }

    @PostMapping
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<DiningAreaResponse>> createDiningArea(
            @Valid @RequestBody CreateDiningAreaRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<DiningAreaResponse>builder()
                        .message("Dining area created successfully")
                        .data(diningAreaService.createDiningArea(request, currentUser))
                        .build()
        );
    }

    @PutMapping("/{areaId}")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<DiningAreaResponse>> updateDiningArea(
            @PathVariable Integer areaId,
            @Valid @RequestBody UpdateDiningAreaRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<DiningAreaResponse>builder()
                        .message("Dining area updated successfully")
                        .data(diningAreaService.updateDiningArea(areaId, request, currentUser))
                        .build()
        );
    }

    @DeleteMapping("/{areaId}")
    @Secured({"ROLE_ADMIN"})
    public ResponseEntity<ApiResponse<Void>> deleteDiningArea(
            @PathVariable Integer areaId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        diningAreaService.deleteDiningArea(areaId, currentUser);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Dining area deleted successfully")
                        .build()
        );
    }
}
