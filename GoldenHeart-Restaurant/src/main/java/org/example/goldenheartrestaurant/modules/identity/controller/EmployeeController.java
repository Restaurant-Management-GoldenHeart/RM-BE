package org.example.goldenheartrestaurant.modules.identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.identity.dto.request.CreateEmployeeRequest;
import org.example.goldenheartrestaurant.modules.identity.dto.request.UpdateEmployeeRequest;
import org.example.goldenheartrestaurant.modules.identity.dto.request.UpdateOwnEmployeeProfileRequest;
import org.example.goldenheartrestaurant.modules.identity.dto.response.EmployeeResponse;
import org.example.goldenheartrestaurant.modules.identity.dto.response.EmployeeSelfResponse;
import org.example.goldenheartrestaurant.modules.identity.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
/**
 * Controller quản lý nhân viên.
 *
 * Tầng controller chịu trách nhiệm chính:
 * - nhận request/response HTTP
 * - validate input bằng @Valid
 * - chặn quyền thô bằng @PreAuthorize
 *
 * Các rule nghiệp vụ tinh hơn vẫn để trong EmployeeService để tránh phụ thuộc hoàn toàn vào annotation.
 */
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> getEmployees(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<EmployeeResponse>>builder()
                        .message("Employees retrieved successfully")
                        .data(employeeService.getEmployees(keyword, page, size))
                        .build()
        );
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeById(@PathVariable Integer employeeId) {
        return ResponseEntity.ok(
                ApiResponse.<EmployeeResponse>builder()
                        .message("Employee retrieved successfully")
                        .data(employeeService.getEmployeeById(employeeId))
                        .build()
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<EmployeeResponse>builder()
                        .message("Employee created successfully")
                        .data(employeeService.createEmployee(request, currentUser))
                        .build()
        );
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable Integer employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<EmployeeResponse>builder()
                        .message("Employee updated successfully")
                        .data(employeeService.updateEmployee(employeeId, request, currentUser))
                        .build()
        );
    }

    @DeleteMapping("/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(
            @PathVariable Integer employeeId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        employeeService.deleteEmployee(employeeId, currentUser);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Employee deleted successfully")
                        .build()
        );
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EmployeeSelfResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        // currentUser được đổ vào từ SecurityContext sau khi JwtAuthenticationFilter chạy xong.
        return ResponseEntity.ok(
                ApiResponse.<EmployeeSelfResponse>builder()
                        .message("My profile retrieved successfully")
                        .data(employeeService.getMyProfile(currentUser.getUserId()))
                        .build()
        );
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EmployeeSelfResponse>> updateMyProfile(
            @Valid @RequestBody UpdateOwnEmployeeProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<EmployeeSelfResponse>builder()
                        .message("My profile updated successfully")
                        .data(employeeService.updateMyProfile(currentUser.getUserId(), request))
                        .build()
        );
    }
}
