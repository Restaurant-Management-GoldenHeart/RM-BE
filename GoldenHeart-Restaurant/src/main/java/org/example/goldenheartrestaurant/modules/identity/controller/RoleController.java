package org.example.goldenheartrestaurant.modules.identity.controller;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.modules.identity.dto.response.RoleResponse;
import org.example.goldenheartrestaurant.modules.identity.repository.RoleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
/**
 * Read-only role lookup endpoint used by admin/manager screens.
 */
public class RoleController {

    private final RoleRepository roleRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles() {
        List<RoleResponse> roles = roleRepository.findAllByOrderByNameAsc()
                .stream()
                .map(role -> new RoleResponse(role.getId(), role.getName(), role.getDescription()))
                .toList();

        return ResponseEntity.ok(
                ApiResponse.<List<RoleResponse>>builder()
                        .message("Roles retrieved successfully")
                        .data(roles)
                        .build()
        );
    }
}
