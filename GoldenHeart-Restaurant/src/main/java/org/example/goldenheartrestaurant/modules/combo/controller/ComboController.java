package org.example.goldenheartrestaurant.modules.combo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.combo.dto.request.CreateComboRequest;
import org.example.goldenheartrestaurant.modules.combo.dto.request.UpdateComboRequest;
import org.example.goldenheartrestaurant.modules.combo.dto.response.ComboResponse;
import org.example.goldenheartrestaurant.modules.combo.service.ComboService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    // STAFF cũng được đọc để hiển thị combo trong POS
    @GetMapping
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF"})
    public ResponseEntity<ApiResponse<List<ComboResponse>>> listCombos(
            @RequestParam(required = false) Integer branchId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.<List<ComboResponse>>builder()
                .message("Lấy danh sách combo thành công")
                .data(comboService.listByBranch(branchId, currentUser))
                .build());
    }

    @GetMapping("/{id}")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF"})
    public ResponseEntity<ApiResponse<ComboResponse>> getCombo(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.<ComboResponse>builder()
                .message("Lấy thông tin combo thành công")
                .data(comboService.getById(id, currentUser))
                .build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<ComboResponse>> createCombo(
            @Valid @RequestPart("payload") CreateComboRequest request,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<ComboResponse>builder()
                .message("Tạo combo thành công")
                .data(comboService.createCombo(request, imageFile, currentUser))
                .build());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<ComboResponse>> updateCombo(
            @PathVariable Integer id,
            @Valid @RequestPart("payload") UpdateComboRequest request,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.<ComboResponse>builder()
                .message("Cập nhật combo thành công")
                .data(comboService.updateCombo(id, request, imageFile, currentUser))
                .build());
    }

    @DeleteMapping("/{id}")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<Void>> deleteCombo(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        comboService.deleteCombo(id, currentUser);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Xóa combo thành công")
                .build());
    }
}
