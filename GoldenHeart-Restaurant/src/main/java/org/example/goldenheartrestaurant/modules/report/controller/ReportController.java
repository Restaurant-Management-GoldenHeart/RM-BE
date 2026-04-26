package org.example.goldenheartrestaurant.modules.report.controller;

import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.response.ApiResponse;
import org.example.goldenheartrestaurant.common.security.CustomUserDetails;
import org.example.goldenheartrestaurant.modules.report.dto.request.ReportGroupBy;
import org.example.goldenheartrestaurant.modules.report.dto.request.ReportPeriodType;
import org.example.goldenheartrestaurant.modules.report.dto.response.BillStatusSummaryResponse;
import org.example.goldenheartrestaurant.modules.report.dto.response.DashboardReportResponse;
import org.example.goldenheartrestaurant.modules.report.dto.response.PaymentMethodBreakdownResponse;
import org.example.goldenheartrestaurant.modules.report.dto.response.RevenueSummaryResponse;
import org.example.goldenheartrestaurant.modules.report.dto.response.RevenueTimeseriesResponse;
import org.example.goldenheartrestaurant.modules.report.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_KITCHEN"})
    public ResponseEntity<ApiResponse<DashboardReportResponse>> getDashboardReport(
            @RequestParam(required = false) Integer branchId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<DashboardReportResponse>builder()
                        .message("Dashboard report retrieved successfully")
                        .data(reportService.getDashboardReport(branchId, currentUser))
                        .build()
        );
    }

    @GetMapping("/revenue/summary")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<RevenueSummaryResponse>> getRevenueSummary(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) ReportPeriodType periodType,
            @RequestParam(required = false) LocalDate anchorDate,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<RevenueSummaryResponse>builder()
                        .message("Revenue summary retrieved successfully")
                        .data(reportService.getRevenueSummary(branchId, periodType, anchorDate, currentUser))
                        .build()
        );
    }

    @GetMapping("/revenue/timeseries")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<RevenueTimeseriesResponse>> getRevenueTimeseries(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) ReportGroupBy groupBy,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<RevenueTimeseriesResponse>builder()
                        .message("Revenue timeseries retrieved successfully")
                        .data(reportService.getRevenueTimeseries(branchId, fromDate, toDate, groupBy, currentUser))
                        .build()
        );
    }

    @GetMapping("/payments/method-breakdown")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<PaymentMethodBreakdownResponse>> getPaymentMethodBreakdown(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) ReportPeriodType periodType,
            @RequestParam(required = false) LocalDate anchorDate,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<PaymentMethodBreakdownResponse>builder()
                        .message("Payment method breakdown retrieved successfully")
                        .data(reportService.getPaymentMethodBreakdown(branchId, periodType, anchorDate, currentUser))
                        .build()
        );
    }

    @GetMapping("/bills/status-summary")
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public ResponseEntity<ApiResponse<BillStatusSummaryResponse>> getBillStatusSummary(
            @RequestParam(required = false) Integer branchId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.<BillStatusSummaryResponse>builder()
                        .message("Bill status summary retrieved successfully")
                        .data(reportService.getBillStatusSummary(branchId, currentUser))
                        .build()
        );
    }
}
