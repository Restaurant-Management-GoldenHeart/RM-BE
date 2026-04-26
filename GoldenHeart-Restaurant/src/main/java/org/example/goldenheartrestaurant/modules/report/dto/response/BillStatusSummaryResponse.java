package org.example.goldenheartrestaurant.modules.report.dto.response;

import java.time.LocalDateTime;

public record BillStatusSummaryResponse(
        Integer branchId,
        String branchName,
        Long unpaidBills,
        Long partialBills,
        Long paidBills,
        Long totalBills,
        LocalDateTime generatedAt
) {
}
