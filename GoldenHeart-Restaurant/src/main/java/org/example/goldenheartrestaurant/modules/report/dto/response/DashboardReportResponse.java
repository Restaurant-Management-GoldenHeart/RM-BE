package org.example.goldenheartrestaurant.modules.report.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DashboardReportResponse(
        Integer branchId,
        String branchName,
        Long totalEmployees,
        Long totalCustomers,
        Long totalMenuItems,
        Long totalInventoryItems,
        BigDecimal totalInventoryValue,
        Long lowStockItems,
        Long outOfStockItems,
        Long availableTables,
        Long occupiedTables,
        Long reservedTables,
        Long cleaningTables,
        Long activeOrders,
        Long pendingKitchenItems,
        Long processingKitchenItems,
        Long waitingStockItems,
        Long todayPaymentCount,
        Long todayPaidBills,
        BigDecimal todayCashIn,
        BigDecimal todayPaidBillRevenue,
        BigDecimal todayGrossProfit,
        BigDecimal todayAveragePaidBillValue,
        LocalDateTime generatedAt
) {
}
