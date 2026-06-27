package org.example.goldenheartrestaurant.modules.waste.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.response.PageResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.waste.dto.request.CreateWasteRequestRequest;
import org.example.goldenheartrestaurant.modules.waste.dto.request.ReviewWasteRequestRequest;
import org.example.goldenheartrestaurant.modules.waste.dto.response.WasteRequestResponse;
import org.example.goldenheartrestaurant.modules.waste.dto.response.WasteRequestSummaryResponse;
import org.example.goldenheartrestaurant.modules.waste.dto.response.WasteStatsResponse;
import org.example.goldenheartrestaurant.modules.waste.entity.WasteRequestStatus;
import org.example.goldenheartrestaurant.modules.waste.service.WasteRequestService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/waste-requests")
@RequiredArgsConstructor
public class WasteRequestController {

    private final WasteRequestService wasteRequestService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<WasteRequestResponse>> create(
            @Valid @RequestPart("payload") CreateWasteRequestRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        WasteRequestResponse response = wasteRequestService.create(request, images, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<WasteRequestResponse>builder()
                        .message("Phiếu xuất hủy đã được gửi, đang chờ duyệt")
                        .data(response)
                        .build()
        );
    }

    @GetMapping
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<PageResponse<WasteRequestSummaryResponse>>> list(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) WasteRequestStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        PageResponse<WasteRequestSummaryResponse> result =
                wasteRequestService.list(branchId, status, dateFrom, dateTo, page, size, currentUser);
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<WasteRequestSummaryResponse>>builder()
                        .message("Danh sách phiếu xuất hủy")
                        .data(result)
                        .build()
        );
    }

    @GetMapping("/stats")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<WasteStatsResponse>> getStats(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<WasteStatsResponse>builder()
                        .message("Thống kê phiếu xuất hủy")
                        .data(wasteRequestService.getStats(branchId, dateFrom, dateTo, currentUser))
                        .build()
        );
    }

    @GetMapping("/export")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        byte[] excel = wasteRequestService.exportExcel(branchId, dateFrom, dateTo, currentUser);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=waste_report.xlsx")
                .body(excel);
    }

    @GetMapping("/{id}")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<WasteRequestResponse>> getById(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<WasteRequestResponse>builder()
                        .message("Chi tiết phiếu xuất hủy")
                        .data(wasteRequestService.getById(id, currentUser))
                        .build()
        );
    }

    @PutMapping("/{id}/approve")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<WasteRequestResponse>> approve(
            @PathVariable Integer id,
            @RequestBody(required = false) ReviewWasteRequestRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<WasteRequestResponse>builder()
                        .message("Phiếu xuất hủy đã được duyệt, kho đã được trừ")
                        .data(wasteRequestService.approve(id, request, currentUser))
                        .build()
        );
    }

    @PutMapping("/{id}/reject")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<WasteRequestResponse>> reject(
            @PathVariable Integer id,
            @RequestBody(required = false) ReviewWasteRequestRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<WasteRequestResponse>builder()
                        .message("Phiếu xuất hủy đã bị từ chối")
                        .data(wasteRequestService.reject(id, request, currentUser))
                        .build()
        );
    }

    @GetMapping("/pending-count")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<Long>> countPending(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<Long>builder()
                        .message("Số phiếu đang chờ duyệt")
                        .data(wasteRequestService.countPending(currentUser))
                        .build()
        );
    }
}
